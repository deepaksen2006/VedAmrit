from rest_framework.routers import DefaultRouter
from .views import (HospitalViewSet, DoctorViewSet, ManagedHospitalViewSet, ManagedDoctorViewSet,
                    DecisionViewSet, DoctorDecisionViewSet, MembershipViewSet, AffiliationViewSet, AuditViewSet, LocationCandidateViewSet)

router = DefaultRouter()
router.register("hospitals", HospitalViewSet, basename="care-hospital")
router.register("doctors", DoctorViewSet, basename="care-doctor")
router.register("manage/hospitals", ManagedHospitalViewSet, basename="care-manage-hospital")
router.register("manage/doctors", ManagedDoctorViewSet, basename="care-manage-doctor")
router.register("manage/affiliations", AffiliationViewSet, basename="care-affiliation")
router.register("admin/hospitals", DecisionViewSet, basename="care-hospital-decision")
router.register("admin/doctors", DoctorDecisionViewSet, basename="care-doctor-decision")
router.register("admin/memberships", MembershipViewSet, basename="care-membership")
router.register("admin/audits", AuditViewSet, basename="care-audit")
router.register("admin/location-candidates", LocationCandidateViewSet, basename="care-location-candidate")
urlpatterns = router.urls
