from django.db.models import Q
from rest_framework.exceptions import PermissionDenied
from rest_framework.permissions import BasePermission, SAFE_METHODS

from apps.doctors.models import DoctorProfile
from .models import Hospital, HospitalMembership


def is_directory_admin(user):
    return bool(user and user.is_authenticated and user.is_active and (user.is_superuser or
        user.has_perm("community_care.manage_directory") or user.has_perm("community_care.verify_directory")))


def can_verify(user):
    return bool(user and user.is_authenticated and user.is_active and
                (user.is_superuser or user.has_perm("community_care.verify_directory")))


def hospital_scope(user, *, write=False):
    if is_directory_admin(user):
        return Hospital.objects.all()
    if not user or not user.is_authenticated or not user.is_active:
        return Hospital.objects.none()
    query = Q(memberships__user=user, memberships__is_active=True)
    if write:
        query &= Q(memberships__role=HospitalMembership.Role.ADMIN)
    return Hospital.objects.filter(query).distinct()


def doctor_scope(user, *, write=False):
    if is_directory_admin(user):
        return DoctorProfile.objects.all()
    return DoctorProfile.objects.filter(care_affiliations__hospital__in=hospital_scope(user, write=write),
                                         care_affiliations__is_active=True).distinct()


def require_directory_admin(user):
    if not is_directory_admin(user):
        raise PermissionDenied("An authorized VedAmrit directory administrator is required.")


class DirectoryAccess(BasePermission):
    """Public GET only; writes also enforce scoped querysets in the views/services."""
    def has_permission(self, request, view):
        return request.method in SAFE_METHODS or bool(request.user and request.user.is_authenticated)
