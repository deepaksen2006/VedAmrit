from rest_framework import serializers

from apps.doctors.models import DoctorProfile
from apps.doctors.serializers import DoctorProfileSerializer
from .models import Consultation


class ConsultationSerializer(serializers.ModelSerializer):
    doctor_detail = DoctorProfileSerializer(source="doctor", read_only=True)

    class Meta:
        model = Consultation
        fields = [
            "id",
            "patient",
            "doctor",
            "doctor_detail",
            "symptoms",
            "dosha_type",
            "lifestyle_data",
            "existing_diet_plan",
            "consultation_notes",
            "wellness_advice",
            "follow_up_date",
            "created_at",
        ]
        read_only_fields = ["id", "patient", "doctor_detail", "created_at"]

    def validate_doctor(self, value: DoctorProfile):
        if not value.is_verified:
            raise serializers.ValidationError("Only verified doctors can receive consultations.")
        return value

    def create(self, validated_data):
        validated_data["patient"] = self.context["request"].user
        return super().create(validated_data)
