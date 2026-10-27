from decimal import Decimal

from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models
from django.db.models.functions import Lower

from .base import VerificationStatus, VerifiedRecord
from .validators import validate_phone, validate_postal_code, validate_string_list


class Hospital(VerifiedRecord):
    class FacilityType(models.TextChoices):
        UNSPECIFIED = "unspecified", "Unspecified"
        GOVERNMENT_TEACHING = "government_teaching_hospital", "Government teaching hospital"
        GOVERNMENT_HOSPITAL = "government_hospital", "Government hospital"
        GOVERNMENT_DISPENSARY = "government_dispensary", "Government dispensary"
        PRIVATE_HOSPITAL = "private_hospital", "Private hospital"
        PRIVATE_CLINIC = "private_clinic", "Private clinic"
        PRIVATE_TREATMENT = "private_treatment_centre", "Private treatment centre"

    class ImportReview(models.TextChoices):
        NOT_IMPORTED = "NOT_IMPORTED", "Not imported"
        NEEDS_REVIEW = "NEEDS_REVIEW", "Source and location review required"
        REVIEWED = "REVIEWED", "Import reviewed by VedAmrit administrator"

    name = models.CharField(max_length=180)
    description = models.TextField(blank=True, max_length=10000)
    address = models.CharField(max_length=240)
    city = models.CharField(max_length=80, db_index=True)
    state = models.CharField(max_length=80, db_index=True)
    district = models.CharField(max_length=80, blank=True, db_index=True)
    facility_type = models.CharField(max_length=32, choices=FacilityType.choices, default=FacilityType.UNSPECIFIED)
    postal_code = models.CharField(max_length=12, validators=[validate_postal_code])
    latitude = models.DecimalField(max_digits=9, decimal_places=6, null=True, blank=True,
                                  validators=[MinValueValidator(Decimal("-90")), MaxValueValidator(Decimal("90"))])
    longitude = models.DecimalField(max_digits=10, decimal_places=6, null=True, blank=True,
                                   validators=[MinValueValidator(Decimal("-180")), MaxValueValidator(Decimal("180"))])
    phone = models.CharField(max_length=20, blank=True, validators=[validate_phone])
    email = models.EmailField(blank=True)
    facilities = models.JSONField(default=list, blank=True, validators=[validate_string_list])
    listed_hours_unverified = models.TextField(blank=True, max_length=2000, help_text="Source listing only; never doctor availability or appointment slots.")
    services_unverified = models.TextField(blank=True, max_length=4000)
    source_namespace = models.CharField(max_length=80, blank=True, editable=False)
    source_record_id = models.CharField(max_length=80, blank=True, editable=False)
    source_url = models.URLField(max_length=1000, blank=True, help_text="Leave blank unless independently checked.")
    import_provenance = models.JSONField(default=dict, blank=True, editable=False)
    import_review_status = models.CharField(max_length=20, choices=ImportReview.choices, default=ImportReview.NOT_IMPORTED)
    import_review_notes = models.TextField(blank=True, max_length=4000)
    booking_enabled = models.BooleanField(default=False, editable=False)
    location_status = models.CharField(max_length=12, choices=[("UNVERIFIED", "Unverified"), ("APPROVED", "Approved"), ("STALE", "Stale")], default="UNVERIFIED", editable=False)
    approved_location = models.ForeignKey("LocationCandidate", null=True, blank=True, on_delete=models.PROTECT,
                                           related_name="approved_hospitals", editable=False)
    care_record_type = "HOSPITAL"
    critical_fields = ("name", "description", "address", "city", "state", "postal_code", "latitude", "longitude", "phone", "email", "facilities",
                       "district", "facility_type", "source_url", "listed_hours_unverified", "services_unverified", "import_review_status", "import_review_notes")

    class Meta:
        ordering = ["name", "id"]
        permissions = [("manage_directory", "Manage the VedAmrit care directory"),
                       ("verify_directory", "Verify the VedAmrit care directory")]
        indexes = [models.Index(fields=["verification_status", "is_active", "city"], name="care_hospital_public_idx")]
        constraints = [
            models.UniqueConstraint(fields=["source_namespace", "source_record_id"], condition=~models.Q(source_record_id=""), name="care_hospital_source_unique"),
            models.CheckConstraint(condition=models.Q(source_namespace="", source_record_id="") | (~models.Q(source_namespace="") & ~models.Q(source_record_id="")), name="care_hospital_source_pair"),
            models.CheckConstraint(condition=models.Q(booking_enabled=False), name="care_booking_disabled"),
            models.CheckConstraint(condition=models.Q(import_review_status__in=["NOT_IMPORTED", "NEEDS_REVIEW", "REVIEWED"]), name="care_valid_import_review"),
            models.UniqueConstraint(Lower("name"), Lower("address"), Lower("city"), name="care_hospital_identity_ci"),
            models.CheckConstraint(condition=models.Q(verification_status__in=VerificationStatus.values), name="care_hospital_valid_status"),
            models.CheckConstraint(condition=models.Q(latitude__isnull=True, longitude__isnull=True) | models.Q(latitude__isnull=False, longitude__isnull=False), name="care_coordinates_pair"),
            models.CheckConstraint(condition=models.Q(latitude__isnull=True) | models.Q(latitude__gte=-90, latitude__lte=90), name="care_latitude_range"),
            models.CheckConstraint(condition=models.Q(longitude__isnull=True) | models.Q(longitude__gte=-180, longitude__lte=180), name="care_longitude_range"),
        ]

    def clean(self):
        super().clean()
        validate_string_list(self.facilities)
        if (self.latitude is None) != (self.longitude is None):
            raise ValidationError({"latitude": "Supply both coordinates or neither."})
        if self.source_record_id and self.import_review_status == self.ImportReview.NOT_IMPORTED:
            raise ValidationError({"import_review_status": "Imported provenance cannot be removed from the review workflow."})
        if self.import_review_status == self.ImportReview.REVIEWED and len(self.import_review_notes.strip()) < 10:
            raise ValidationError({"import_review_notes": "Describe independent source/location checks and duplicate resolution (at least 10 characters)."})

    def validate_ready_for_review(self):
        if not self.phone.strip():
            raise ValidationError({"phone": "Confirm a valid contact number before submission/verification."})
        validate_phone(self.phone)
        if self.import_review_status == self.ImportReview.NEEDS_REVIEW:
            raise ValidationError({"import_review_status": "A VedAmrit administrator must resolve source, location and possible-duplicate issues before submission/verification."})
        from .duplicate_review import duplicate_is_clear
        if not duplicate_is_clear(self):
            raise ValidationError({"duplicate_review": "Resolve and record the possible duplicate identity before submission/verification."})

    def save(self, *args, **kwargs):
        fields = kwargs.get("update_fields")
        if self.pk:
            previous = type(self).objects.filter(pk=self.pk).first()
            geographic_fields = ("name", "address", "city", "district", "state", "postal_code", "latitude", "longitude")
            if previous and previous.approved_location_id and any(
                (fields is None or name in fields) and getattr(previous, name) != getattr(self, name) for name in geographic_fields):
                self.location_status = "STALE"
                if fields is not None:
                    kwargs["update_fields"] = set(fields) | {"location_status"}
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.name} — {self.city}"


class HospitalMembership(models.Model):
    class Role(models.TextChoices):
        ADMIN = "ADMIN", "Hospital administrator"
        STAFF = "STAFF", "Hospital staff (read only)"

    hospital = models.ForeignKey(Hospital, on_delete=models.PROTECT, related_name="memberships")
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="care_memberships")
    role = models.CharField(max_length=10, choices=Role.choices)
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["id"]
        constraints = [models.UniqueConstraint(fields=["hospital", "user"], name="care_unique_membership"),
                       models.CheckConstraint(condition=models.Q(role__in=["ADMIN", "STAFF"]), name="care_membership_valid_role")]
        indexes = [models.Index(fields=["user", "is_active", "role"], name="care_membership_scope_idx")]


class DoctorHospitalAffiliation(models.Model):
    class Mode(models.TextChoices):
        IN_PERSON = "IN_PERSON", "In person"
        ONLINE = "ONLINE", "Online"
        BOTH = "BOTH", "In person and online"

    doctor = models.ForeignKey("doctors.DoctorProfile", on_delete=models.PROTECT, related_name="care_affiliations")
    hospital = models.ForeignKey(Hospital, on_delete=models.PROTECT, related_name="doctor_affiliations")
    consultation_mode = models.CharField(max_length=12, choices=Mode.choices, default=Mode.IN_PERSON)
    fees = models.DecimalField(max_digits=10, decimal_places=2, validators=[MinValueValidator(Decimal("0"))])
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["id"]
        constraints = [models.UniqueConstraint(fields=["doctor", "hospital"], name="care_unique_affiliation"),
                       models.CheckConstraint(condition=models.Q(fees__gte=0), name="care_nonnegative_fee"),
                       models.CheckConstraint(condition=models.Q(consultation_mode__in=["IN_PERSON", "ONLINE", "BOTH"]), name="care_valid_consultation_mode")]
        indexes = [models.Index(fields=["hospital", "is_active"], name="care_affiliation_public_idx")]


class VerificationAudit(models.Model):
    actor = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL)
    record_type = models.CharField(max_length=10, choices=[("HOSPITAL", "Hospital"), ("DOCTOR", "Doctor")])
    record_id = models.PositiveBigIntegerField()
    action = models.CharField(max_length=32)
    previous_status = models.CharField(max_length=12, blank=True)
    new_status = models.CharField(max_length=12, choices=VerificationStatus.choices)
    reason = models.TextField(blank=True)
    details = models.JSONField(default=dict)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-timestamp", "-id"]
        indexes = [models.Index(fields=["record_type", "record_id", "timestamp"], name="care_audit_record_idx")]
        constraints = [models.CheckConstraint(condition=models.Q(record_type__in=["HOSPITAL", "DOCTOR"]), name="care_audit_valid_type")]

    def save(self, *args, **kwargs):
        if self.pk:
            raise ValidationError("Verification audit records are append only.")
        super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise ValidationError("Verification audit records are append only.")


class GeocodingRun(models.Model):
    hospital = models.ForeignKey(Hospital, on_delete=models.PROTECT, related_name="geocoding_runs")
    query_hash = models.CharField(max_length=64)
    query = models.TextField()
    snapshot = models.JSONField()
    reference_context = models.JSONField(default=dict, blank=True)
    status = models.CharField(max_length=16, choices=[("PENDING", "Pending"), ("SUCCESS", "Success"), ("NO_RESULTS", "No results"), ("FAILED", "Failed")], default="PENDING")
    error_code = models.CharField(max_length=40, blank=True)
    retry_after = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=["hospital", "query_hash"], name="care_geocode_query_unique")]


class LocationCandidate(models.Model):
    run = models.ForeignKey(GeocodingRun, on_delete=models.PROTECT, related_name="candidates")
    fingerprint = models.CharField(max_length=64)
    latitude = models.DecimalField(max_digits=9, decimal_places=6, validators=[MinValueValidator(Decimal("-90")), MaxValueValidator(Decimal("90"))])
    longitude = models.DecimalField(max_digits=10, decimal_places=6, validators=[MinValueValidator(Decimal("-180")), MaxValueValidator(Decimal("180"))])
    confidence = models.FloatField(null=True, blank=True, validators=[MinValueValidator(0), MaxValueValidator(1)])
    provider = models.CharField(max_length=32, default="MapTiler Cloud", editable=False)
    provider_reference = models.CharField(max_length=255, blank=True)
    label = models.CharField(max_length=1000)
    feature_types = models.JSONField(default=list)
    attribution = models.TextField()
    flags = models.JSONField(default=list)
    distance_from_reference_km = models.FloatField(null=True, blank=True)
    status = models.CharField(max_length=12, choices=[("PROPOSED", "Proposed"), ("APPROVED", "Approved"), ("REJECTED", "Rejected")], default="PROPOSED")
    reviewed_by = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL)
    review_reason = models.TextField(blank=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)
    evidence_url = models.URLField(max_length=1000, blank=True)
    based_on_candidate = models.ForeignKey("self", null=True, blank=True, on_delete=models.PROTECT, related_name="manual_corrections")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["run_id", "id"]
        constraints = [models.UniqueConstraint(fields=["run", "fingerprint"], name="care_location_candidate_unique"),
                       models.CheckConstraint(condition=models.Q(latitude__gte=-90, latitude__lte=90, longitude__gte=-180, longitude__lte=180), name="care_candidate_coord_range")]


class HospitalDuplicateReview(models.Model):
    class Status(models.TextChoices):
        NEEDS_REVIEW = "NEEDS_REVIEW", "Needs review"
        DISTINCT_CONFIRMED = "DISTINCT_CONFIRMED", "Confirmed distinct facility"
        DUPLICATE_CONFIRMED = "DUPLICATE_CONFIRMED", "Confirmed duplicate (retained)"

    hospital = models.OneToOneField(Hospital, on_delete=models.PROTECT, related_name="duplicate_review")
    status = models.CharField(max_length=24, choices=Status.choices, default=Status.NEEDS_REVIEW)
    related_hospital = models.ForeignKey(Hospital, null=True, blank=True, on_delete=models.PROTECT, related_name="related_duplicate_reviews")
    snapshot = models.JSONField(default=dict, blank=True)
    evidence_url = models.URLField(max_length=1000, blank=True)
    reviewed_by = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL)
    reviewed_at = models.DateTimeField(null=True, blank=True)
    reason = models.TextField(blank=True)
    revision = models.PositiveIntegerField(default=1)

    class Meta:
        constraints = [models.CheckConstraint(condition=models.Q(status__in=["NEEDS_REVIEW", "DISTINCT_CONFIRMED", "DUPLICATE_CONFIRMED"]), name="care_duplicate_review_status")]
