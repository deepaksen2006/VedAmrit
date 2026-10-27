from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import include, path

from apps.agentic_reports.views import PatientProfileView

urlpatterns = [
    path("admin/", admin.site.urls),
    path("", include("apps.doctors.urls")),
    path("", include("apps.consultations.urls")),
    path("", include("apps.dietplans.urls")),
    path("api/auth/", include("apps.authentication.urls")),
    path("api/profile/", PatientProfileView.as_view(), name="patient-profile"),
    path("api/agentic/", include("apps.agentic_reports.urls")),
    path("api/care/", include("apps.community_care.urls")),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
