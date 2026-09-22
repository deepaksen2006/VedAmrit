from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework.test import APIClient
from unittest.mock import patch

from apps.authentication.models import FirebaseIdentity


class PatientAuthenticationApiTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.user = get_user_model().objects.create_user(
            username="patient@example.com",
            email="patient@example.com",
            password="correct-password",
        )

    def test_register_returns_jwt_tokens(self):
        response = self.client.post(
            "/api/auth/register/",
            {"identifier": "new@example.com", "password": "strong-password"},
            format="json",
        )

        self.assertEqual(response.status_code, 201)
        self.assertIn("access", response.data)
        self.assertIn("refresh", response.data)

    def test_register_creates_user_with_hashed_password(self):
        response = self.client.post(
            "/api/auth/register/",
            {"identifier": "hashcheck@example.com", "password": "strong-password"},
            format="json",
        )

        self.assertEqual(response.status_code, 201)
        user = get_user_model().objects.get(username="hashcheck@example.com")
        self.assertEqual(user.email, "hashcheck@example.com")
        self.assertNotEqual(user.password, "strong-password")
        self.assertTrue(user.check_password("strong-password"))

    def test_register_rejects_duplicate_email(self):
        response = self.client.post(
            "/api/auth/register/",
            {"identifier": "patient@example.com", "password": "strong-password"},
            format="json",
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("identifier", response.data)

    def test_register_rejects_invalid_email(self):
        response = self.client.post(
            "/api/auth/register/",
            {"identifier": "not-an-email", "password": "strong-password"},
            format="json",
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("identifier", response.data)

    def test_register_rejects_weak_password(self):
        response = self.client.post(
            "/api/auth/register/",
            {"identifier": "weak@example.com", "password": "short"},
            format="json",
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("password", response.data)

    def test_login_uses_identifier_and_password(self):
        response = self.client.post(
            "/api/auth/login/",
            {"identifier": "patient@example.com", "password": "correct-password"},
            format="json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertIn("access", response.data)
        self.assertIn("refresh", response.data)

    def test_incorrect_credentials_are_rejected(self):
        response = self.client.post(
            "/api/auth/login/",
            {"identifier": "patient@example.com", "password": "wrong"},
            format="json",
        )

        self.assertEqual(response.status_code, 400)

    def test_refresh_endpoint_returns_new_access_token(self):
        login = self.client.post(
            "/api/auth/login/",
            {"identifier": "patient@example.com", "password": "correct-password"},
            format="json",
        )

        response = self.client.post(
            "/api/auth/token/refresh/",
            {"refresh": login.data["refresh"]},
            format="json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertIn("access", response.data)


class FirebaseAuthenticationApiTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    def test_protected_profile_rejects_missing_token(self):
        response = self.client.get("/api/profile/")

        self.assertEqual(response.status_code, 401)

    @patch("apps.authentication.drf_auth.verify_firebase_id_token", side_effect=ValueError("bad token"))
    def test_protected_profile_rejects_invalid_firebase_token(self, _verify):
        self.client.credentials(HTTP_AUTHORIZATION="Bearer invalid-firebase-token")

        response = self.client.get("/api/profile/")

        self.assertEqual(response.status_code, 401)

    @patch(
        "apps.authentication.drf_auth.verify_firebase_id_token",
        return_value={"uid": "firebase-uid-123", "email": "patient.firebase@example.com"},
    )
    def test_valid_firebase_token_creates_mapping_and_accesses_profile(self, _verify):
        self.client.credentials(HTTP_AUTHORIZATION="Bearer valid-firebase-token")

        response = self.client.get("/api/profile/")

        self.assertEqual(response.status_code, 200)
        identity = FirebaseIdentity.objects.select_related("user").get(firebase_uid="firebase-uid-123")
        self.assertEqual(identity.email, "patient.firebase@example.com")
        self.assertEqual(response.data["user_id"], identity.user_id)

    @patch(
        "apps.authentication.drf_auth.verify_firebase_id_token",
        return_value={"uid": "firebase-uid-123", "email": "updated.firebase@example.com"},
    )
    def test_existing_firebase_mapping_is_reused(self, _verify):
        user = get_user_model().objects.create_user(username="firebase:firebase-uid-123")
        FirebaseIdentity.objects.create(
            user=user,
            firebase_uid="firebase-uid-123",
            email="old.firebase@example.com",
        )
        self.client.credentials(HTTP_AUTHORIZATION="Bearer valid-firebase-token")

        response = self.client.get("/api/profile/")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["user_id"], user.id)
        self.assertEqual(get_user_model().objects.count(), 1)
        self.assertEqual(
            FirebaseIdentity.objects.get(firebase_uid="firebase-uid-123").email,
            "updated.firebase@example.com",
        )
