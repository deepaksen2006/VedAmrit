from decimal import Decimal
from django.contrib.auth import get_user_model
from rest_framework import serializers

from apps.doctors.models import DoctorProfile
from .models import Hospital, HospitalMembership, DoctorHospitalAffiliation, VerificationAudit, LocationCandidate
from .validators import validate_phone, validate_registration


class StrictInput:
    def to_internal_value(self, data):
        if not isinstance(data, dict):
            raise serializers.ValidationError("Expected a JSON object.")
        forbidden = set(data) - {name for name, field in self.fields.items() if not field.read_only}
        if forbidden:
            raise serializers.ValidationError({key: "Unknown or read-only field." for key in sorted(forbidden)})
        return super().to_internal_value(data)


class HospitalSerializer(StrictInput, serializers.ModelSerializer):
    # Preserve the existing management-create contract; only imports allow a missing draft phone.
    phone = serializers.CharField(max_length=20, validators=[validate_phone])
    duplicate_review_status = serializers.SerializerMethodField()

    def get_duplicate_review_status(self, obj):
        from .models import HospitalDuplicateReview
        review = HospitalDuplicateReview.objects.filter(hospital=obj).first()
        return review.status if review else "NEEDS_REVIEW" if obj.import_provenance.get("duplicate_candidates") else "NOT_FLAGGED"

    class Meta:
        model = Hospital
        fields = ["id", "name", "description", "address", "city", "state", "postal_code", "latitude", "longitude",
                  "phone", "email", "facilities", "verification_status", "is_active", "revision", "created_by", "created_at", "updated_at",
                  "district", "facility_type", "listed_hours_unverified", "services_unverified", "source_namespace", "source_record_id",
                  "source_url", "import_provenance", "import_review_status", "import_review_notes", "booking_enabled", "location_status", "approved_location", "duplicate_review_status"]
        read_only_fields = ["id", "verification_status", "revision", "created_by", "created_at", "updated_at",
                            "source_namespace", "source_record_id", "import_provenance", "booking_enabled", "location_status", "approved_location"]
        # Model full_clean() validates expression-based uniqueness on both create and patch.
        validators = []

    def validate_phone(self, value):
        validate_phone(value)
        return value


class PublicHospitalSerializer(serializers.ModelSerializer):
    location_status = serializers.SerializerMethodField()
    location_attribution = serializers.SerializerMethodField()

    def approved_position(self, obj):
        candidate = obj.approved_location
        return (obj.location_status == "APPROVED" and candidate is not None
                and candidate.status == "APPROVED" and candidate.run.hospital_id == obj.pk
                and obj.latitude is not None and obj.longitude is not None
                and obj.latitude == candidate.latitude and obj.longitude == candidate.longitude)

    def get_location_status(self, obj):
        return "APPROVED" if self.approved_position(obj) else "UNVERIFIED"

    def get_location_attribution(self, obj):
        return obj.approved_location.attribution if self.approved_position(obj) else ""

    def to_representation(self, instance):
        data = super().to_representation(instance)
        # Verified identity alone does not attest to a map position.
        if not self.approved_position(instance):
            data["latitude"] = data["longitude"] = None
        return data

    class Meta:
        model = Hospital
        fields = ["id", "name", "description", "address", "city", "state", "postal_code", "latitude", "longitude",
                  "phone", "email", "facilities", "verification_status", "updated_at", "district", "facility_type", "booking_enabled",
                  "location_status", "location_attribution"]


class NearbyHospitalSerializer(PublicHospitalSerializer):
    distance_km = serializers.FloatField(read_only=True)

    class Meta(PublicHospitalSerializer.Meta):
        fields = PublicHospitalSerializer.Meta.fields + ["distance_km"]


class LocationReviewSerializer(StrictInput, serializers.Serializer):
    expected_revision = serializers.IntegerField(min_value=1)
    reason = serializers.CharField(min_length=10, max_length=2000)
    exact_location_confirmed = serializers.BooleanField(default=False)
    duplicate_review_resolved = serializers.BooleanField(default=False)
    evidence_url = serializers.URLField(max_length=1000, required=False, allow_blank=True)


class LocationCandidateSerializer(serializers.ModelSerializer):
    hospital_id = serializers.IntegerField(source="run.hospital_id", read_only=True)
    hospital_revision = serializers.IntegerField(source="run.hospital.revision", read_only=True)
    hospital_name = serializers.CharField(source="run.hospital.name", read_only=True)

    class Meta:
        model = LocationCandidate
        fields = ["id", "hospital_id", "hospital_name", "hospital_revision", "latitude", "longitude", "confidence", "provider",
                  "provider_reference", "label", "feature_types", "attribution", "flags", "distance_from_reference_km", "status",
                  "reviewed_by", "review_reason", "reviewed_at", "created_at", "evidence_url", "based_on_candidate"]
        read_only_fields = fields


class PublicAffiliationSerializer(serializers.ModelSerializer):
    hospital_id = serializers.IntegerField(read_only=True)
    hospital_name = serializers.CharField(source="hospital.name", read_only=True)
    city = serializers.CharField(source="hospital.city", read_only=True)
    verification_status = serializers.CharField(source="hospital.verification_status", read_only=True)

    class Meta:
        model = DoctorHospitalAffiliation
        fields = ["id", "hospital_id", "hospital_name", "city", "consultation_mode", "fees", "verification_status"]


class PublicDoctorSerializer(serializers.ModelSerializer):
    credentials_reviewed_at = serializers.DateTimeField(read_only=True)
    registration_number = serializers.CharField(source="bams_registration_number", read_only=True)
    experience_years = serializers.IntegerField(source="experience", read_only=True)
    affiliations = PublicAffiliationSerializer(source="public_affiliations", many=True, read_only=True)

    class Meta:
        model = DoctorProfile
        # No user IDs, private certificates, email/phone, creator or internal metadata.
        fields = ["id", "full_name", "qualifications", "specialization", "registration_number", "registration_council",
                  "experience_years", "bio", "verification_status", "updated_at", "credentials_reviewed_at", "affiliations"]


class DoctorSerializer(StrictInput, serializers.ModelSerializer):
    registration_number = serializers.CharField(source="bams_registration_number", max_length=80, validators=[validate_registration])
    experience_years = serializers.IntegerField(source="experience", min_value=0, max_value=80)
    hospital_id = serializers.IntegerField(write_only=True, required=False, min_value=1)
    user_id = serializers.PrimaryKeyRelatedField(source="user", queryset=get_user_model().objects.filter(is_active=True),
                                               write_only=True, required=False, allow_null=True)
    consultation_mode = serializers.ChoiceField(choices=DoctorHospitalAffiliation.Mode.choices, write_only=True, required=False)
    fees = serializers.DecimalField(max_digits=10, decimal_places=2, min_value=Decimal("0"), write_only=True, required=False)

    class Meta:
        model = DoctorProfile
        fields = ["id", "full_name", "phone", "email", "registration_number", "registration_council", "qualifications",
                  "specialization", "experience_years", "clinic_name", "consultation_fee", "bio", "profile_metadata",
                  "available_timings", "verification_status", "is_active", "revision", "created_by", "created_at", "updated_at",
                  "hospital_id", "user_id", "consultation_mode", "fees"]
        read_only_fields = ["id", "verification_status", "revision", "created_by", "created_at", "updated_at"]
        validators = []

    def validate_phone(self, value):
        validate_phone(value)
        return value

    def validate_registration_number(self, value):
        return value.strip().upper()

    def validate_email(self, value):
        return value.strip().lower()

    def validate(self, attrs):
        if self.instance and set(attrs).intersection({"hospital_id", "user", "consultation_mode", "fees"}):
            raise serializers.ValidationError("Manage affiliations separately; doctor account linkage cannot be changed here.")
        return attrs


class TransitionSerializer(StrictInput, serializers.Serializer):
    expected_revision = serializers.IntegerField(min_value=1)
    reason = serializers.CharField(min_length=3, max_length=2000, trim_whitespace=True)


class CredentialReviewSerializer(StrictInput, serializers.Serializer):
    # References may be council URLs or controlled institutional document IDs.
    # Never put certificate contents, personal identity documents or secrets here.
    evidence_references = serializers.ListField(
        child=serializers.CharField(min_length=3, max_length=1000, trim_whitespace=True), min_length=1, max_length=10)
    registration_checked = serializers.BooleanField()
    qualifications_checked = serializers.BooleanField()
    profile_claims_checked = serializers.BooleanField()
    affiliations_checked = serializers.BooleanField()

    def validate(self, attrs):
        for field in ("registration_checked", "qualifications_checked", "profile_claims_checked", "affiliations_checked"):
            if not attrs[field]:
                raise serializers.ValidationError({field: "An independent human verifier must complete this check before approval."})
        return attrs


class DoctorTransitionSerializer(TransitionSerializer):
    credential_review = CredentialReviewSerializer(required=False)


class DoctorUserLinkSerializer(TransitionSerializer):
    user_id = serializers.PrimaryKeyRelatedField(source="user", queryset=get_user_model().objects.filter(is_active=True))


class MembershipSerializer(StrictInput, serializers.ModelSerializer):
    class Meta:
        model = HospitalMembership
        fields = ["id", "hospital", "user", "role", "is_active", "created_at", "updated_at"]
        read_only_fields = ["id", "created_at", "updated_at"]
        validators = []


class AffiliationSerializer(StrictInput, serializers.ModelSerializer):
    hospital_id = serializers.IntegerField(min_value=1)
    doctor_id = serializers.IntegerField(min_value=1)

    class Meta:
        model = DoctorHospitalAffiliation
        fields = ["id", "hospital_id", "doctor_id", "consultation_mode", "fees", "is_active", "created_at", "updated_at"]
        read_only_fields = ["id", "created_at", "updated_at"]
        validators = []


class AuditSerializer(serializers.ModelSerializer):
    class Meta:
        model = VerificationAudit
        fields = ["id", "actor", "record_type", "record_id", "action", "previous_status", "new_status", "reason", "details", "timestamp"]
        read_only_fields = fields
