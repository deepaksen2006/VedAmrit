from django.contrib.auth import authenticate, get_user_model
from rest_framework import serializers
from rest_framework_simplejwt.tokens import RefreshToken

from .models import DoctorProfile

User = get_user_model()


class DoctorProfileSerializer(serializers.ModelSerializer):
    class Meta:
        model = DoctorProfile
        fields = [
            "id",
            "full_name",
            "phone",
            "email",
            "bams_registration_number",
            "specialization",
            "experience",
            "clinic_name",
            "consultation_fee",
            "bio",
            "profile_image",
            "degree_certificate",
            "is_verified",
            "is_online",
            "available_timings",
            "created_at",
        ]
        read_only_fields = ["id", "is_verified", "created_at"]


class DoctorRegisterSerializer(serializers.Serializer):
    full_name = serializers.CharField(max_length=160)
    phone = serializers.CharField(max_length=20)
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8)
    bams_registration_number = serializers.CharField(max_length=80)
    specialization = serializers.CharField(max_length=140)
    experience = serializers.IntegerField(min_value=0, max_value=70)
    clinic_name = serializers.CharField(max_length=180)
    consultation_fee = serializers.DecimalField(max_digits=8, decimal_places=2)
    bio = serializers.CharField(required=False, allow_blank=True)
    profile_image = serializers.ImageField(required=False, allow_null=True)
    degree_certificate = serializers.FileField(required=False, allow_null=True)

    def validate_email(self, value):
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError("Email is already registered.")
        return value

    def validate_phone(self, value):
        if DoctorProfile.objects.filter(phone=value).exists():
            raise serializers.ValidationError("Phone is already registered.")
        return value

    def validate_bams_registration_number(self, value):
        if DoctorProfile.objects.filter(bams_registration_number=value).exists():
            raise serializers.ValidationError("BAMS registration number is already registered.")
        return value

    def create(self, validated_data):
        password = validated_data.pop("password")
        user = User.objects.create_user(
            username=validated_data["email"],
            email=validated_data["email"],
            password=password,
            first_name=validated_data["full_name"],
        )
        profile = DoctorProfile.objects.create(user=user, **validated_data)
        return profile


class DoctorLoginSerializer(serializers.Serializer):
    identifier = serializers.CharField()
    password = serializers.CharField(write_only=True)

    def validate(self, attrs):
        identifier = attrs["identifier"]
        password = attrs["password"]
        user = User.objects.filter(email=identifier).first()
        if user is None:
            profile = DoctorProfile.objects.filter(phone=identifier).select_related("user").first()
            user = profile.user if profile else None
        if user is None:
            raise serializers.ValidationError("Invalid credentials.")
        user = authenticate(username=user.username, password=password)
        if user is None or not hasattr(user, "doctor_profile"):
            raise serializers.ValidationError("Invalid doctor credentials.")
        attrs["user"] = user
        return attrs

    def to_representation(self, instance):
        user = instance["user"]
        refresh = RefreshToken.for_user(user)
        return {
            "refresh": str(refresh),
            "access": str(refresh.access_token),
            "is_verified": user.doctor_profile.is_verified,
        }


class DoctorStatusSerializer(serializers.Serializer):
    is_online = serializers.BooleanField()
    available_timings = serializers.CharField(required=False, allow_blank=True, max_length=160)
