from rest_framework import generics

from apps.doctors.permissions import IsDoctor
from .models import Consultation
from .serializers import ConsultationSerializer


class ConsultationCreateView(generics.CreateAPIView):
    serializer_class = ConsultationSerializer


class DoctorConsultationListView(generics.ListAPIView):
    serializer_class = ConsultationSerializer
    permission_classes = [IsDoctor]

    def get_queryset(self):
        return Consultation.objects.filter(doctor=self.request.user.doctor_profile).select_related("doctor", "patient")
