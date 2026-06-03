from rest_framework import generics

from apps.doctors.permissions import IsVerifiedDoctor
from .models import DietPlan
from .serializers import DietPlanSerializer


class DietPlanCreateView(generics.CreateAPIView):
    serializer_class = DietPlanSerializer
    permission_classes = [IsVerifiedDoctor]


class PatientDietPlanListView(generics.ListAPIView):
    serializer_class = DietPlanSerializer

    def get_queryset(self):
        return DietPlan.objects.filter(patient=self.request.user).select_related("doctor")
