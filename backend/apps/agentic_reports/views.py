from __future__ import annotations

from django.shortcuts import get_object_or_404
from rest_framework import permissions, status
from rest_framework.exceptions import NotFound, PermissionDenied, ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from agent_engine.contracts import AgentEngineInputError
from apps.agentic_reports.assessment_sync import persist_assessment

from apps.agentic_reports.models import (
    AgentRunStatus,
    AssessmentResult,
    DoctorAssignment,
    DoctorReviewStatus,
    PatientProfile,
    WellnessPlan,
)
from apps.agentic_reports.serializers import (
    AgenticPlanGenerationRequestSerializer,
    AssessmentResultSerializer,
    CanonicalAssessmentRequestSerializer,
    DoctorChangesRequestSerializer,
    DoctorDecisionRequestSerializer,
    DoctorReviewSerializer,
    FinalReportSerializer,
    PatientProfileSerializer,
    DoctorAssignmentSerializer,
    WellnessPlanListSerializer,
    WellnessPlanSerializer,
    WellnessPlanStatusSerializer,
)
from apps.agentic_reports.services import (
    AgentRunFailed,
    create_plan_revision,
    create_wellness_plan,
    finalize_report,
    record_doctor_review,
)


def _is_verified_doctor(user) -> bool:
    return bool(
        user
        and user.is_authenticated
        and hasattr(user, "doctor_profile")
        and user.doctor_profile.is_verified
    )


def _patient_queryset(user):
    return (
        WellnessPlan.objects.filter(user=user)
        .select_related("assessment", "agent_run", "parent_plan")
        .order_by("-created_at")
    )


def _doctor_queryset():
    return (
        WellnessPlan.objects.select_related(
            "assessment", "agent_run", "user", "parent_plan", "assignment"
        )
        .filter(doctor_status=DoctorReviewStatus.PENDING)
        .order_by("-created_at")
    )


class PatientProfileView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get_object(self):
        profile, _created = PatientProfile.objects.get_or_create(user=self.request.user)
        return profile

    def get(self, request):
        serializer = PatientProfileSerializer(self.get_object())
        return Response(serializer.data)

    def put(self, request):
        serializer = PatientProfileSerializer(self.get_object(), data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)

    def patch(self, request):
        serializer = PatientProfileSerializer(self.get_object(), data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)


class DoctorAssignmentListCreateView(APIView):
    permission_classes = [permissions.IsAdminUser]

    def get(self, request):
        assignments = DoctorAssignment.objects.select_related(
            "patient_profile", "doctor"
        ).order_by("-created_at")
        return Response(DoctorAssignmentSerializer(assignments, many=True).data)

    def post(self, request):
        serializer = DoctorAssignmentSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        assignment = serializer.save()
        WellnessPlan.objects.filter(
            user=assignment.patient_profile.user,
            assignment__isnull=True,
        ).update(assignment=assignment)
        return Response(
            DoctorAssignmentSerializer(assignment).data,
            status=status.HTTP_201_CREATED,
        )


class AssessmentResultListCreateView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        assessments = AssessmentResult.objects.filter(user=request.user).order_by("-created_at")
        return Response(AssessmentResultSerializer(assessments, many=True).data)

    def post(self, request):
        serializer = CanonicalAssessmentRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        assessment, created = persist_assessment(request.user, data)
        return Response(AssessmentResultSerializer(assessment).data, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)


class LatestAssessmentResultView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        assessment = AssessmentResult.objects.filter(user=request.user).order_by("-created_at", "-pk").first()
        if assessment is None:
            raise NotFound("Assessment result not found.")
        return Response(AssessmentResultSerializer(assessment).data)


def _get_plan_for_user_or_doctor(request, pk: int) -> WellnessPlan:
    queryset = WellnessPlan.objects.select_related("assessment", "agent_run", "user", "parent_plan")
    try:
        plan = queryset.get(pk=pk)
    except WellnessPlan.DoesNotExist as exc:
        raise NotFound("Plan not found.") from exc
    if plan.user_id == request.user.id:
        return plan
    if (
        _is_verified_doctor(request.user)
        and plan.assignment_id
        and plan.assignment.doctor_id == request.user.doctor_profile.id
        and plan.assignment.active
    ):
        return plan
    raise NotFound("Plan not found.")


def _get_plan_for_verified_doctor(request, pk: int) -> WellnessPlan:
    if not _is_verified_doctor(request.user):
        raise PermissionDenied("Verified doctor access is required.")
    return get_object_or_404(
        WellnessPlan.objects.select_related(
            "assessment", "agent_run", "user", "parent_plan", "assignment"
        ).filter(
            assignment__doctor=request.user.doctor_profile,
            assignment__active=True,
        ),
        pk=pk,
    )


class AgenticPlanListCreateView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        serializer = WellnessPlanListSerializer(_patient_queryset(request.user), many=True)
        return Response(serializer.data)

    def post(self, request):
        serializer = AgenticPlanGenerationRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        payload = dict(serializer.validated_data)
        try:
            result = create_wellness_plan(request.user, payload)
        except AgentEngineInputError as exc:
            raise ValidationError(exc.as_dict()) from exc
        except AgentRunFailed as exc:
            return Response(
                {
                    "detail": "Agent execution failed.",
                    "agent_run_id": exc.agent_run.id,
                    "error_code": exc.agent_run.error_code,
                    "error_message": exc.agent_run.error_message,
                },
                status=status.HTTP_502_BAD_GATEWAY,
            )
        return Response(
            WellnessPlanSerializer(result.wellness_plan, context={"request": request}).data,
            status=status.HTTP_201_CREATED,
        )


class AgenticPlanDetailView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk: int):
        plan = _get_plan_for_user_or_doctor(request, pk)
        return Response(WellnessPlanSerializer(plan, context={"request": request}).data)


class AgenticPlanStatusView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk: int):
        plan = _get_plan_for_user_or_doctor(request, pk)
        return Response(WellnessPlanStatusSerializer(plan).data)


class DoctorReviewQueueView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        if not _is_verified_doctor(request.user):
            raise PermissionDenied("Verified doctor access is required.")
        serializer = WellnessPlanListSerializer(
            _doctor_queryset().filter(
                assignment__doctor=request.user.doctor_profile,
                assignment__active=True,
            ),
            many=True,
        )
        return Response(serializer.data)


class DoctorPlanDetailView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk: int):
        plan = _get_plan_for_verified_doctor(request, pk)
        return Response(WellnessPlanSerializer(plan, context={"request": request}).data)


class DoctorApprovePlanView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        plan = _get_plan_for_verified_doctor(request, pk)
        serializer = DoctorDecisionRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        review = record_doctor_review(
            wellness_plan=plan,
            doctor=request.user.doctor_profile,
            decision=DoctorReviewStatus.APPROVED,
            feedback=serializer.validated_data.get("feedback", ""),
            comments=serializer.validated_data.get("comments", ""),
        )
        plan.refresh_from_db()
        return Response(
            {
                "plan": WellnessPlanStatusSerializer(plan).data,
                "review": DoctorReviewSerializer(review).data,
            }
        )


class DoctorRequestChangesView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        plan = _get_plan_for_verified_doctor(request, pk)
        serializer = DoctorChangesRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        review = record_doctor_review(
            wellness_plan=plan,
            doctor=request.user.doctor_profile,
            decision=DoctorReviewStatus.CHANGES_REQUESTED,
            feedback=serializer.validated_data["feedback"],
            comments=serializer.validated_data.get("comments", ""),
        )
        revision_validation = {
            **(plan.validation or {}),
            "status": "REVIEW_REQUIRED",
            "doctor_feedback": serializer.validated_data["feedback"],
            "revision_source": "doctor_changes_requested",
        }
        revision = create_plan_revision(
            parent_plan=plan,
            validation=revision_validation,
            workflow_status="PENDING_DOCTOR_REVIEW",
        )
        return Response(
            {
                "review": DoctorReviewSerializer(review).data,
                "revision": WellnessPlanSerializer(revision, context={"request": request}).data,
            },
            status=status.HTTP_201_CREATED,
        )


class DoctorRejectPlanView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        plan = _get_plan_for_verified_doctor(request, pk)
        serializer = DoctorDecisionRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        review = record_doctor_review(
            wellness_plan=plan,
            doctor=request.user.doctor_profile,
            decision=DoctorReviewStatus.REJECTED,
            feedback=serializer.validated_data.get("feedback", ""),
            comments=serializer.validated_data.get("comments", ""),
        )
        plan.refresh_from_db()
        return Response(
            {
                "plan": WellnessPlanStatusSerializer(plan).data,
                "review": DoctorReviewSerializer(review).data,
            }
        )


class AgenticPlanReportView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk: int):
        plan = _get_plan_for_user_or_doctor(request, pk)
        if plan.doctor_status != DoctorReviewStatus.APPROVED:
            return Response(
                {
                    "detail": "Final report is not available until doctor approval is complete.",
                    "doctor_status": plan.doctor_status,
                },
                status=status.HTTP_409_CONFLICT,
            )
        report = getattr(plan, "final_report_record", None)
        if report is None:
            report = finalize_report(
                wellness_plan=plan,
                report_path=f"agentic://wellness-plans/{plan.id}/final-report",
                metadata={
                    "plan_id": plan.id,
                    "version": plan.version,
                    "lineage_id": str(plan.lineage_id),
                    "workflow_status": plan.workflow_status,
                },
            )
        return Response(FinalReportSerializer(report, context={"request": request}).data)
