from decimal import Decimal
from django.conf import settings
from django.db import models
from django.db.models.functions import Lower
from django.core.validators import MaxValueValidator, MinValueValidator
from apps.community_care.base import VerifiedRecord, VerificationStatus
from apps.community_care.validators import validate_metadata, validate_phone, validate_registration, validate_string_list


class DoctorProfile(VerifiedRecord):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL, related_name="doctor_profile")
    full_name = models.CharField(max_length=160)
    phone = models.CharField(max_length=20, unique=True, validators=[validate_phone])
    email = models.EmailField(unique=True)
    bams_registration_number = models.CharField(max_length=80, unique=True, validators=[validate_registration])
    registration_council = models.CharField(max_length=180, blank=True)
    qualifications = models.JSONField(default=list, blank=True, validators=[validate_string_list])
    profile_metadata = models.JSONField(default=dict, blank=True, validators=[validate_metadata])
    specialization = models.CharField(max_length=140)
    experience = models.PositiveIntegerField(help_text="Experience in years", validators=[MaxValueValidator(80)])
    clinic_name = models.CharField(max_length=180)
    consultation_fee = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(Decimal("0"))])
    bio = models.TextField(blank=True)
    profile_image = models.ImageField(upload_to="doctor_profiles/", blank=True, null=True)
    degree_certificate = models.FileField(upload_to="doctor_certificates/", blank=True, null=True)
    is_verified = models.BooleanField(default=False)
    is_online = models.BooleanField(default=False)
    available_timings = models.CharField(max_length=160, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    care_record_type = "DOCTOR"
    critical_fields = ("user", "full_name", "phone", "email", "bams_registration_number", "registration_council", "qualifications",
                       "specialization", "experience", "clinic_name", "bio", "profile_image", "degree_certificate", "profile_metadata")

    class Meta:
        ordering = ["-is_online", "full_name"]
        indexes = [models.Index(fields=["verification_status", "is_active", "specialization"], name="care_doctor_public_idx")]
        constraints = [models.UniqueConstraint(Lower("bams_registration_number"), name="care_doctor_registration_ci"),
                       models.UniqueConstraint(Lower("email"), name="care_doctor_email_ci"),
                       models.CheckConstraint(condition=models.Q(verification_status__in=VerificationStatus.values), name="care_doctor_valid_status"),
                       models.CheckConstraint(condition=models.Q(is_verified=True, verification_status="VERIFIED") |
                           (models.Q(is_verified=False) & ~models.Q(verification_status="VERIFIED")), name="care_doctor_verification_sync")]

    def __str__(self):
        return f"{self.full_name} ({self.bams_registration_number})"

    def clean(self):
        super().clean()
        validate_string_list(self.qualifications)
        validate_metadata(self.profile_metadata)
