from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from apps.authentication.views import PatientLoginView, PatientRegisterView


urlpatterns = [
    path("register/", PatientRegisterView.as_view(), name="patient-register"),
    path("login/", PatientLoginView.as_view(), name="patient-login"),
    path("token/refresh/", TokenRefreshView.as_view(), name="patient-token-refresh"),
]
