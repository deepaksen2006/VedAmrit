from rest_framework import permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView

from apps.authentication.serializers import PatientLoginSerializer, PatientRegisterSerializer


class PatientRegisterView(APIView):
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = PatientRegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        return Response(serializer.to_representation(user), status=status.HTTP_201_CREATED)


class PatientLoginView(APIView):
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = PatientLoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.to_representation(serializer.validated_data))
