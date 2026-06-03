from django.urls import path

from .views import DietPlanCreateView, PatientDietPlanListView

urlpatterns = [
    path("diet-plan/create", DietPlanCreateView.as_view()),
    path("patient/diet-plan", PatientDietPlanListView.as_view()),
]
