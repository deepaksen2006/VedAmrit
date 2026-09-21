from django.urls import path

from apps.agentic_reports import views


urlpatterns = [
    path("plans/", views.AgenticPlanListCreateView.as_view(), name="agentic-plan-list-create"),
    path("plans/<int:pk>/", views.AgenticPlanDetailView.as_view(), name="agentic-plan-detail"),
    path("plans/<int:pk>/status/", views.AgenticPlanStatusView.as_view(), name="agentic-plan-status"),
    path("plans/<int:pk>/report/", views.AgenticPlanReportView.as_view(), name="agentic-plan-report"),
    path("doctor/review-queue/", views.DoctorReviewQueueView.as_view(), name="agentic-doctor-review-queue"),
    path("doctor/plans/<int:pk>/", views.DoctorPlanDetailView.as_view(), name="agentic-doctor-plan-detail"),
    path("doctor/plans/<int:pk>/approve/", views.DoctorApprovePlanView.as_view(), name="agentic-doctor-approve"),
    path("doctor/plans/<int:pk>/request-changes/", views.DoctorRequestChangesView.as_view(), name="agentic-doctor-request-changes"),
    path("doctor/plans/<int:pk>/reject/", views.DoctorRejectPlanView.as_view(), name="agentic-doctor-reject"),
]
