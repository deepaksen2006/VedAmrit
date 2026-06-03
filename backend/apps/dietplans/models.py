from django.conf import settings
from django.db import models

from apps.doctors.models import DoctorProfile


class DietPlan(models.Model):
    patient = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="diet_plans")
    doctor = models.ForeignKey(DoctorProfile, on_delete=models.CASCADE, related_name="created_diet_plans")
    prakriti = models.CharField(max_length=80)
    vikriti = models.CharField(max_length=80)
    agni = models.CharField(max_length=80)
    ama = models.CharField(max_length=80)
    breakfast = models.TextField()
    lunch = models.TextField()
    dinner = models.TextField()
    foods_to_eat = models.TextField()
    foods_to_avoid = models.TextField()
    hydration_advice = models.TextField()
    yoga_recommendation = models.TextField()
    sleep_advice = models.TextField()
    source_inputs = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return f"Diet plan for patient {self.patient_id} by {self.doctor.full_name}"
