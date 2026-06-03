from rest_framework import serializers

from .models import DietPlan


class DietPlanSerializer(serializers.ModelSerializer):
    class Meta:
        model = DietPlan
        fields = [
            "id",
            "patient",
            "doctor",
            "prakriti",
            "vikriti",
            "agni",
            "ama",
            "breakfast",
            "lunch",
            "dinner",
            "foods_to_eat",
            "foods_to_avoid",
            "hydration_advice",
            "yoga_recommendation",
            "sleep_advice",
            "source_inputs",
            "created_at",
        ]
        read_only_fields = ["id", "doctor", "created_at"]

    def validate(self, attrs):
        doctor = self.context["request"].user.doctor_profile
        if not doctor.is_verified:
            raise serializers.ValidationError("Only verified doctors can create diet plans.")
        return attrs

    def create(self, validated_data):
        validated_data["doctor"] = self.context["request"].user.doctor_profile
        return super().create(validated_data)
