from django.urls import path

from .views import (
    DoctorLoginView,
    DoctorProfileUpdateView,
    DoctorProfileView,
    DoctorRegisterView,
    DoctorStatusView,
    VerifiedDoctorListView,
)

urlpatterns = [
    path("doctor/register", DoctorRegisterView.as_view()),
    path("doctor/login", DoctorLoginView.as_view()),
    path("doctors/verified", VerifiedDoctorListView.as_view()),
    path("doctor/profile", DoctorProfileView.as_view()),
    path("doctor/profile/update", DoctorProfileUpdateView.as_view()),
    path("doctor/status", DoctorStatusView.as_view()),
    path("api/doctors/verified", VerifiedDoctorListView.as_view()),
]
