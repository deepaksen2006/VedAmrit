from django.conf import settings
from django.db import models

from apps.doctors.models import DoctorProfile


class Consultation(models.Model):
    patient = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="patient_consultations")
    doctor = models.ForeignKey(DoctorProfile, on_delete=models.CASCADE, related_name="doctor_consultations")
    symptoms = models.TextField()
    dosha_type = models.CharField(max_length=80)
    lifestyle_data = models.JSONField(default=dict, blank=True)
    existing_diet_plan = models.JSONField(default=dict, blank=True)
    consultation_notes = models.TextField(blank=True)
    wellness_advice = models.TextField(blank=True)
    follow_up_date = models.DateField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.patient_id} with {self.doctor.full_name}"
