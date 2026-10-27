from django.db.models import Prefetch, Q, OuterRef, Subquery
from django.shortcuts import get_object_or_404
from rest_framework import mixins, permissions, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import ValidationError
from rest_framework.pagination import PageNumberPagination
from rest_framework.response import Response

from apps.doctors.models import DoctorProfile
from . import services
from .models import Hospital, DoctorHospitalAffiliation, HospitalMembership, VerificationAudit
from .permissions import DirectoryAccess, doctor_scope, hospital_scope, require_directory_admin
from .serializers import (HospitalSerializer, PublicHospitalSerializer, DoctorSerializer, PublicDoctorSerializer,
                          MembershipSerializer, AffiliationSerializer, TransitionSerializer, AuditSerializer, DoctorUserLinkSerializer,
                          DoctorTransitionSerializer)


class CarePagination(PageNumberPagination):
    page_size = 20
    page_size_query_param = "page_size"
    max_page_size = 100


def public_hospitals():
    return Hospital.objects.filter(verification_status="VERIFIED", is_active=True,
                                   import_review_status__in=["NOT_IMPORTED", "REVIEWED"]).exclude(
                                       duplicate_review__status__in=["NEEDS_REVIEW", "DUPLICATE_CONFIRMED"]).select_related("approved_location__run")


def public_affiliations():
    return DoctorHospitalAffiliation.objects.filter(is_active=True, hospital__in=public_hospitals()).select_related("hospital")


def public_doctors():
    # A legacy flag or manually saved status cannot substitute for credential review.
    # A later critical edit/suspension/rejection invalidates the last approval.
    decision = VerificationAudit.objects.filter(record_type="DOCTOR", record_id=OuterRef("pk"),
        action__in=["VERIFY", "REVERIFY_REQUIRED", "SUSPEND", "REOPEN", "REJECT"]).order_by("-id")
    return DoctorProfile.objects.filter(verification_status="VERIFIED", is_verified=True, is_active=True,
        care_affiliations__in=public_affiliations()).annotate(
            credential_decision_id=Subquery(decision.values("pk")[:1]),
            credentials_reviewed_at=Subquery(decision.values("timestamp")[:1]),
        ).filter(credential_decision_id__in=VerificationAudit.objects.filter(
            action="VERIFY", details__credential_review__protocol="doctor-credentials-v1").values("pk")).distinct().prefetch_related(
            Prefetch("care_affiliations", queryset=public_affiliations(), to_attr="public_affiliations"))


class DirectoryViewSet(mixins.ListModelMixin, mixins.RetrieveModelMixin, mixins.CreateModelMixin,
                       mixins.UpdateModelMixin, viewsets.GenericViewSet):
    permission_classes = [DirectoryAccess]
    pagination_class = CarePagination
    http_method_names = ["get", "post", "patch", "head", "options"]
    management = False
    is_hospital = True

    def get_serializer_class(self):
        if self.management or self.action not in ("list", "retrieve"):
            return HospitalSerializer if self.is_hospital else DoctorSerializer
        return PublicHospitalSerializer if self.is_hospital else PublicDoctorSerializer

    def get_queryset(self):
        user = self.request.user
        if self.management or self.action not in ("list", "retrieve"):
            queryset = hospital_scope(user, write=self.action not in ("list", "retrieve")) if self.is_hospital else doctor_scope(user, write=self.action not in ("list", "retrieve"))
        else:
            queryset = public_hospitals() if self.is_hospital else public_doctors()
        params = self.request.query_params
        if self.is_hospital:
            for field in ("district", "facility_type"):
                if params.get(field):
                    queryset = queryset.filter(**{f"{field}__iexact": params[field]})
        affiliation_filters = {}
        for field in ("city", "state"):
            if params.get(field):
                if self.is_hospital:
                    queryset = queryset.filter(**{f"{field}__iexact": params[field]})
                else:
                    affiliation_filters[f"hospital__{field}__iexact"] = params[field]
        if params.get("search"):
            term = params["search"][:200]
            query = Q(name__icontains=term) | Q(city__icontains=term) if self.is_hospital else Q(full_name__icontains=term) | Q(specialization__icontains=term)
            queryset = queryset.filter(query)
        if not self.is_hospital:
            if params.get("specialization"):
                queryset = queryset.filter(specialization__icontains=params["specialization"][:140])
            if params.get("hospital_id"):
                try:
                    hospital_id = int(params["hospital_id"])
                    if hospital_id <= 0:
                        raise ValueError()
                except ValueError:
                    raise ValidationError({"hospital_id": "Use a positive integer."})
                affiliation_filters["hospital_id"] = hospital_id
            if affiliation_filters:
                public = not self.management and self.action in ("list", "retrieve")
                affiliations = public_affiliations() if public else DoctorHospitalAffiliation.objects.filter(
                    hospital__in=hospital_scope(user), is_active=True)
                # All location filters must match the SAME visible/assigned affiliation.
                queryset = queryset.filter(care_affiliations__in=affiliations.filter(**affiliation_filters))
        if self.management and params.get("status"):
            from .base import VerificationStatus
            if params["status"] not in VerificationStatus.values:
                raise ValidationError({"status": "Unknown verification status."})
            queryset = queryset.filter(verification_status=params["status"])
        return queryset.distinct().order_by("name" if self.is_hospital else "full_name", "id")

    def perform_create(self, serializer):
        values = dict(serializer.validated_data)
        if self.is_hospital:
            serializer.instance = services.create_hospital(self.request.user, values)
        else:
            extras = {key: values.pop(key) for key in ("hospital_id", "user", "consultation_mode", "fees") if key in values}
            serializer.instance = services.create_doctor(self.request.user, values, **extras)

    def perform_update(self, serializer):
        serializer.instance = services.update_record(self.request.user, Hospital if self.is_hospital else DoctorProfile,
                                                     serializer.instance.pk, serializer.validated_data)

    @action(detail=True, methods=["post"], permission_classes=[permissions.IsAuthenticated])
    def submit(self, request, pk=None):
        payload = TransitionSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        record = services.transition(request.user, Hospital if self.is_hospital else DoctorProfile, pk, "SUBMIT", **payload.validated_data)
        return Response((HospitalSerializer if self.is_hospital else DoctorSerializer)(record).data)


class HospitalViewSet(DirectoryViewSet):
    @action(detail=False, methods=["get"], permission_classes=[permissions.AllowAny])
    def nearby(self, request):
        from .nearby import nearby_query
        from .serializers import NearbyHospitalSerializer
        queryset = nearby_query(public_hospitals(), request.query_params).select_related("approved_location")
        page = self.paginate_queryset(queryset)
        return self.get_paginated_response(NearbyHospitalSerializer(page, many=True).data)


class DoctorViewSet(DirectoryViewSet):
    is_hospital = False


class ManagedHospitalViewSet(HospitalViewSet):
    permission_classes = [permissions.IsAuthenticated]
    management = True


class ManagedDoctorViewSet(DoctorViewSet):
    permission_classes = [permissions.IsAuthenticated]
    management = True


class DecisionViewSet(viewsets.ViewSet):
    permission_classes = [permissions.IsAuthenticated]
    is_hospital = True

    def decide(self, request, pk, decision):
        payload = (TransitionSerializer if self.is_hospital else DoctorTransitionSerializer)(data=request.data)
        payload.is_valid(raise_exception=True)
        record = services.transition(request.user, Hospital if self.is_hospital else DoctorProfile, pk, decision, **payload.validated_data)
        return Response((HospitalSerializer if self.is_hospital else DoctorSerializer)(record).data)

    @action(detail=True, methods=["post"])
    def verify(self, request, pk=None):
        return self.decide(request, pk, "VERIFY")

    @action(detail=True, methods=["post"])
    def reject(self, request, pk=None):
        return self.decide(request, pk, "REJECT")

    @action(detail=True, methods=["post"])
    def suspend(self, request, pk=None):
        return self.decide(request, pk, "SUSPEND")

    @action(detail=True, methods=["post"])
    def reopen(self, request, pk=None):
        return self.decide(request, pk, "REOPEN")


class DoctorDecisionViewSet(DecisionViewSet):
    is_hospital = False

    @action(detail=True, methods=["post"], url_path="link-user")
    def link_user(self, request, pk=None):
        payload = DoctorUserLinkSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        record = services.link_doctor_user(request.user, pk, **payload.validated_data)
        return Response(DoctorSerializer(record).data)


class MembershipViewSet(mixins.ListModelMixin, mixins.CreateModelMixin, mixins.UpdateModelMixin, viewsets.GenericViewSet):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = MembershipSerializer
    pagination_class = CarePagination
    http_method_names = ["get", "post", "patch", "head", "options"]

    def get_queryset(self):
        require_directory_admin(self.request.user)
        return HospitalMembership.objects.select_related("hospital", "user").order_by("id")

    def perform_create(self, serializer):
        serializer.instance = services.save_membership(self.request.user, serializer.validated_data)

    def perform_update(self, serializer):
        serializer.instance = services.save_membership(self.request.user, serializer.validated_data, serializer.instance.pk)


class AffiliationViewSet(mixins.ListModelMixin, mixins.CreateModelMixin, mixins.UpdateModelMixin, viewsets.GenericViewSet):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = AffiliationSerializer
    pagination_class = CarePagination
    http_method_names = ["get", "post", "patch", "head", "options"]

    def get_queryset(self):
        return DoctorHospitalAffiliation.objects.filter(hospital__in=hospital_scope(self.request.user,
            write=self.action not in ("list", "retrieve"))).order_by("id")

    def perform_create(self, serializer):
        serializer.instance = services.save_affiliation(self.request.user, dict(serializer.validated_data))

    def perform_update(self, serializer):
        serializer.instance = services.save_affiliation(self.request.user, dict(serializer.validated_data), serializer.instance.pk)


class AuditViewSet(mixins.ListModelMixin, viewsets.GenericViewSet):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = AuditSerializer
    pagination_class = CarePagination

    def get_queryset(self):
        require_directory_admin(self.request.user)
        queryset = VerificationAudit.objects.all()
        for field in ("record_type", "record_id"):
            if self.request.query_params.get(field):
                if field == "record_id" and not self.request.query_params[field].isdigit():
                    raise ValidationError({field: "Use a positive integer."})
                queryset = queryset.filter(**{field: self.request.query_params[field]})
        return queryset


class LocationCandidateViewSet(mixins.ListModelMixin, mixins.RetrieveModelMixin, viewsets.GenericViewSet):
    permission_classes = [permissions.IsAuthenticated]
    pagination_class = CarePagination

    def get_serializer_class(self):
        from .serializers import LocationCandidateSerializer
        return LocationCandidateSerializer

    def get_queryset(self):
        from .models import LocationCandidate
        require_directory_admin(self.request.user)
        return LocationCandidate.objects.select_related("run__hospital").order_by("id")

    def decide(self, request, pk, decision):
        from .serializers import LocationReviewSerializer
        from .location_review import review_location
        payload = LocationReviewSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        candidate = review_location(request.user, pk, decision, **payload.validated_data)
        return Response(self.get_serializer(candidate).data)

    @action(detail=True, methods=["post"])
    def approve(self, request, pk=None):
        return self.decide(request, pk, "APPROVE")

    @action(detail=True, methods=["post"])
    def reject(self, request, pk=None):
        return self.decide(request, pk, "REJECT")
