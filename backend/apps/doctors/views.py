from rest_framework import generics, permissions, status
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import DoctorProfile
from .permissions import IsDoctor
from .serializers import (
    DoctorLoginSerializer,
    DoctorProfileSerializer,
    DoctorRegisterSerializer,
    DoctorStatusSerializer,
)


class DoctorRegisterView(generics.CreateAPIView):
    serializer_class = DoctorRegisterSerializer
    permission_classes = [permissions.AllowAny]
    parser_classes = [MultiPartParser, FormParser]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        profile = serializer.save()
        return Response(
            {
                "message": "Doctor registration submitted for BAMS verification.",
                "doctor": DoctorProfileSerializer(profile, context={"request": request}).data,
            },
            status=status.HTTP_201_CREATED,
        )


class DoctorLoginView(APIView):
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = DoctorLoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.to_representation(serializer.validated_data))


class VerifiedDoctorListView(generics.ListAPIView):
    serializer_class = DoctorProfileSerializer
    permission_classes = [permissions.AllowAny]

    def get_queryset(self):
        return DoctorProfile.objects.filter(is_verified=True)


class DoctorProfileView(generics.RetrieveAPIView):
    serializer_class = DoctorProfileSerializer
    permission_classes = [IsDoctor]

    def get_object(self):
        return self.request.user.doctor_profile


class DoctorProfileUpdateView(generics.UpdateAPIView):
    serializer_class = DoctorProfileSerializer
    permission_classes = [IsDoctor]
    parser_classes = [MultiPartParser, FormParser]

    def get_object(self):
        return self.request.user.doctor_profile


class DoctorStatusView(APIView):
    permission_classes = [IsDoctor]

    def put(self, request):
        serializer = DoctorStatusSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        profile = request.user.doctor_profile
        profile.is_online = serializer.validated_data["is_online"]
        profile.available_timings = serializer.validated_data.get("available_timings", profile.available_timings)
        profile.save(update_fields=["is_online", "available_timings"])
        return Response(DoctorProfileSerializer(profile, context={"request": request}).data)
