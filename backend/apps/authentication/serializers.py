from __future__ import annotations

from django.contrib.auth import authenticate, get_user_model
from rest_framework import serializers
from rest_framework_simplejwt.tokens import RefreshToken


User = get_user_model()


class PatientRegisterSerializer(serializers.Serializer):
    identifier = serializers.CharField(max_length=150)
    password = serializers.CharField(write_only=True, min_length=8)

    def validate_identifier(self, value):
        identifier = value.strip()
        if not identifier:
            raise serializers.ValidationError("Identifier is required.")
        if User.objects.filter(username=identifier).exists() or User.objects.filter(email=identifier).exists():
            raise serializers.ValidationError("An account already exists for this identifier.")
        return identifier

    def create(self, validated_data):
        identifier = validated_data["identifier"]
        email = identifier if "@" in identifier else ""
        return User.objects.create_user(
            username=identifier,
            email=email,
            password=validated_data["password"],
        )

    def to_representation(self, user):
        refresh = RefreshToken.for_user(user)
        return {
            "access": str(refresh.access_token),
            "refresh": str(refresh),
            "user": {
                "id": user.id,
                "username": user.username,
                "email": user.email,
            },
        }


class PatientLoginSerializer(serializers.Serializer):
    identifier = serializers.CharField()
    password = serializers.CharField(write_only=True)

    def validate(self, attrs):
        identifier = attrs["identifier"].strip()
        password = attrs["password"]
        user = User.objects.filter(username=identifier).first()
        if user is None and "@" in identifier:
            user = User.objects.filter(email=identifier).first()
        if user is None:
            raise serializers.ValidationError("Invalid credentials.")
        authenticated = authenticate(username=user.username, password=password)
        if authenticated is None:
            raise serializers.ValidationError("Invalid credentials.")
        attrs["user"] = authenticated
        return attrs

    def to_representation(self, instance):
        user = instance["user"]
        refresh = RefreshToken.for_user(user)
        return {
            "access": str(refresh.access_token),
            "refresh": str(refresh),
            "user": {
                "id": user.id,
                "username": user.username,
                "email": user.email,
            },
        }
