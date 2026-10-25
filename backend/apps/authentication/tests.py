from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework.test import APIClient
from datetime import timedelta
from rest_framework_simplejwt.tokens import AccessToken, RefreshToken


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

    def test_login_jwt_authenticates_profile_and_refresh_recovers_expired_access(self):
        login = self.client.post(
            "/api/auth/login/",
            {"identifier": "patient@example.com", "password": "correct-password"},
            format="json",
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {login.data['access']}")
        self.assertEqual(self.client.get("/api/profile/").status_code, 200)

        expired = AccessToken(login.data["access"])
        expired.set_exp(lifetime=timedelta(seconds=-1))
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {expired}")
        self.assertEqual(self.client.get("/api/profile/").status_code, 401)

        self.client.credentials()
        refreshed = self.client.post(
            "/api/auth/token/refresh/", {"refresh": login.data["refresh"]}, format="json"
        )
        self.assertEqual(refreshed.status_code, 200)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {refreshed.data['access']}")
        self.assertEqual(self.client.get("/api/profile/").status_code, 200)

    def test_expired_refresh_is_rejected_and_anonymous_profile_requires_login(self):
        self.assertEqual(self.client.get("/api/profile/").status_code, 401)
        refresh = RefreshToken.for_user(self.user)
        refresh.set_exp(lifetime=timedelta(seconds=-1))
        response = self.client.post(
            "/api/auth/token/refresh/", {"refresh": str(refresh)}, format="json"
        )
        self.assertEqual(response.status_code, 401)
