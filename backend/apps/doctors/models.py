from django.conf import settings
from django.db import models


class DoctorProfile(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="doctor_profile")
    full_name = models.CharField(max_length=160)
    phone = models.CharField(max_length=20, unique=True)
    email = models.EmailField(unique=True)
    bams_registration_number = models.CharField(max_length=80, unique=True)
    specialization = models.CharField(max_length=140)
    experience = models.PositiveIntegerField(help_text="Experience in years")
    clinic_name = models.CharField(max_length=180)
    consultation_fee = models.DecimalField(max_digits=8, decimal_places=2)
    bio = models.TextField(blank=True)
    profile_image = models.ImageField(upload_to="doctor_profiles/", blank=True, null=True)
    degree_certificate = models.FileField(upload_to="doctor_certificates/", blank=True, null=True)
    is_verified = models.BooleanField(default=False)
    is_online = models.BooleanField(default=False)
    available_timings = models.CharField(max_length=160, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-is_online", "full_name"]

    def __str__(self):
        return f"{self.full_name} ({self.bams_registration_number})"
