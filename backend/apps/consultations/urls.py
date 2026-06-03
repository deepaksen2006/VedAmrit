from django.urls import path

from .views import ConsultationCreateView, DoctorConsultationListView

urlpatterns = [
    path("consultation/create", ConsultationCreateView.as_view()),
    path("doctor/consultations", DoctorConsultationListView.as_view()),
]
