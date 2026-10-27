from django import forms
from django.contrib import admin, messages
from django.contrib.admin.helpers import ActionForm
from django.urls import path, reverse
from django.utils.html import format_html
from rest_framework.exceptions import APIException

from apps.doctors.models import DoctorProfile
from .models import Hospital, HospitalMembership, DoctorHospitalAffiliation, VerificationAudit, GeocodingRun, LocationCandidate, HospitalDuplicateReview
from .permissions import is_directory_admin, can_verify
from . import services


class VerificationActionForm(ActionForm):
    reason = forms.CharField(required=False, max_length=2000, label="Decision reason")
    expected_revision = forms.IntegerField(required=False, min_value=1, label="Reviewed revision (one record only)")


class DoctorVerificationActionForm(VerificationActionForm):
    evidence_references = forms.CharField(required=False, max_length=10000, widget=forms.Textarea(attrs={"rows": 2}),
        label="Credential evidence references (one council URL or institutional document ID per line)")
    registration_checked = forms.BooleanField(required=False, label="Registration/current standing checked with issuing council")
    qualifications_checked = forms.BooleanField(required=False, label="Published qualifications independently checked")
    profile_claims_checked = forms.BooleanField(required=False, label="Specialty, experience and biography claims checked")
    affiliations_checked = forms.BooleanField(required=False, label="Hospital affiliations independently checked")


class DirectoryAdmin(admin.ModelAdmin):
    """Care roles alone never grant access to Django Admin or its global records."""
    def has_module_permission(self, request):
        return is_directory_admin(request.user)

    def has_view_permission(self, request, obj=None):
        return is_directory_admin(request.user)

    def has_add_permission(self, request):
        return is_directory_admin(request.user)

    def has_change_permission(self, request, obj=None):
        return is_directory_admin(request.user)

    def has_delete_permission(self, request, obj=None):
        return False


class VerifiedRecordAdmin(DirectoryAdmin):
    action_form = VerificationActionForm
    readonly_fields = ("verification_status", "revision", "created_by", "created_at", "updated_at")
    actions = ("submit_review", "verify_records", "reject_records", "suspend_records", "reopen_records")

    def get_actions(self, request):
        actions = super().get_actions(request)
        if not can_verify(request.user):
            for key in ("verify_records", "reject_records", "suspend_records", "reopen_records"):
                actions.pop(key, None)
        return actions

    def decide(self, request, queryset, action):
        if queryset.count() != 1:
            self.message_user(request, "Select exactly one reviewed record and supply its revision and a decision reason.", messages.ERROR)
            return
        try:
            revision = int(request.POST.get("expected_revision", ""))
            extra = {}
            if self.model is DoctorProfile and action == "VERIFY":
                extra["credential_review"] = {
                    "evidence_references": request.POST.get("evidence_references", "").splitlines(),
                    **{key: request.POST.get(key) == "on" for key in (
                        "registration_checked", "qualifications_checked", "profile_claims_checked", "affiliations_checked")},
                }
            services.transition(request.user, self.model, queryset.get().pk, action, revision, request.POST.get("reason", ""), **extra)
        except (ValueError, APIException) as error:
            self.message_user(request, str(error) or "Supply the reviewed revision and reason.", messages.ERROR)
            return
        self.message_user(request, "Decision recorded in the verification audit.")

    @admin.action(description="Submit selected record for review")
    def submit_review(self, request, queryset):
        self.decide(request, queryset, "SUBMIT")

    @admin.action(description="Verify selected record after independent credential checks")
    def verify_records(self, request, queryset):
        self.decide(request, queryset, "VERIFY")

    @admin.action(description="Reject selected pending record")
    def reject_records(self, request, queryset):
        self.decide(request, queryset, "REJECT")

    @admin.action(description="Suspend selected verified record")
    def suspend_records(self, request, queryset):
        self.decide(request, queryset, "SUSPEND")

    @admin.action(description="Reopen selected suspended record for review")
    def reopen_records(self, request, queryset):
        self.decide(request, queryset, "REOPEN")

    def save_model(self, request, obj, form, change):
        if change:
            values = {key: form.cleaned_data[key] for key in form.changed_data}
            current = services.update_record(request.user, self.model, obj.pk, values)
            obj.__dict__.update(current.__dict__)
        else:
            obj.created_by = request.user
            services.checked_save(obj, request.user)


@admin.register(Hospital)
class HospitalAdmin(VerifiedRecordAdmin):
    list_display = ("id", "name", "city", "facility_type", "verification_status", "import_review_status", "is_active", "revision")
    list_display_links = ("id", "name")
    list_filter = ("verification_status", "import_review_status", "facility_type", "is_active", "state", "district", "city")
    search_fields = ("name", "address", "city", "postal_code", "source_record_id")
    readonly_fields = VerifiedRecordAdmin.readonly_fields + ("source_namespace", "source_record_id", "import_provenance", "booking_enabled", "location_status", "approved_location", "exact_location_workspace")

    def get_urls(self):
        def review_view(request, object_id):
            from .review_admin import hospital_location_review
            return hospital_location_review(self, request, object_id)
        return [path("<int:object_id>/location-review/", self.admin_site.admin_view(review_view),
                     name="community_care_hospital_location_review")] + super().get_urls()

    @admin.display(description="Location / duplicate review")
    def exact_location_workspace(self, obj):
        if obj is None or not obj.pk:
            return "Save a draft before reviewing its location."
        return format_html('<a href="{}">Compare proposals and review an exact location</a>',
                           reverse("admin:community_care_hospital_location_review", args=[obj.pk]))


@admin.register(DoctorProfile)
class DoctorAdmin(VerifiedRecordAdmin):
    action_form = DoctorVerificationActionForm
    list_display = ("id", "full_name", "bams_registration_number", "registration_council", "verification_status", "is_active", "revision")
    list_filter = ("verification_status", "is_active", "specialization")
    search_fields = ("full_name", "bams_registration_number", "registration_council", "email")
    readonly_fields = VerifiedRecordAdmin.readonly_fields + ("is_verified",)

    def get_readonly_fields(self, request, obj=None):
        return self.readonly_fields + (("user", "is_online") if obj else ("is_online",))


@admin.register(HospitalMembership)
class MembershipAdmin(DirectoryAdmin):
    list_display = ("id", "hospital", "user", "role", "is_active")
    list_filter = ("role", "is_active")
    search_fields = ("hospital__name", "user__username")
    readonly_fields = ("created_at", "updated_at")

    def get_readonly_fields(self, request, obj=None):
        return self.readonly_fields + (("hospital", "user") if obj else ())

    def save_model(self, request, obj, form, change):
        fields = ("role", "is_active") if change else ("hospital", "user", "role", "is_active")
        saved = services.save_membership(request.user, {key: getattr(obj, key) for key in fields}, obj.pk if change else None)
        obj.__dict__.update(saved.__dict__)


@admin.register(DoctorHospitalAffiliation)
class AffiliationAdmin(DirectoryAdmin):
    list_display = ("id", "doctor", "hospital", "consultation_mode", "fees", "is_active")
    list_filter = ("consultation_mode", "is_active")
    search_fields = ("doctor__full_name", "hospital__name")
    readonly_fields = ("created_at", "updated_at")

    def get_readonly_fields(self, request, obj=None):
        return self.readonly_fields + (("doctor", "hospital") if obj else ())

    def save_model(self, request, obj, form, change):
        values = {key: getattr(obj, key) for key in ("consultation_mode", "fees", "is_active")}
        if not change:
            values.update(doctor_id=obj.doctor_id, hospital_id=obj.hospital_id)
        saved = services.save_affiliation(request.user, values, obj.pk if change else None)
        obj.__dict__.update(saved.__dict__)


@admin.register(VerificationAudit)
class AuditAdmin(DirectoryAdmin):
    list_display = ("id", "record_type", "record_id", "action", "actor", "previous_status", "new_status", "timestamp")
    list_filter = ("record_type", "action", "new_status")
    search_fields = ("reason", "actor__username")
    readonly_fields = tuple(field.name for field in VerificationAudit._meta.fields)
    actions = None

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False


class LocationActionForm(VerificationActionForm):
    exact_location_confirmed = forms.BooleanField(required=False, label="Exact facility location independently confirmed")
    duplicate_review_resolved = forms.BooleanField(required=False, label="Possible duplicate identities resolved")
    evidence_url = forms.URLField(required=False, max_length=1000, label="Independent evidence URL (required for broad/manual locations)")


@admin.register(LocationCandidate)
class LocationCandidateAdmin(DirectoryAdmin):
    list_display = ("id", "hospital_name", "latitude", "longitude", "confidence", "status", "created_at")
    list_filter = ("status", "run__hospital__district")
    search_fields = ("run__hospital__name", "run__hospital__source_record_id", "label")
    readonly_fields = ("facility_comparison",) + tuple(field.name for field in LocationCandidate._meta.fields) + ("hospital_revision",)
    action_form = LocationActionForm
    actions = ("approve_location", "reject_location")

    def get_queryset(self, request):
        return super().get_queryset(request).select_related("run__hospital", "reviewed_by")

    @admin.display(description="Compare supplied facility and proposed location")
    def facility_comparison(self, obj):
        from .review_forms import map_links
        hospital = obj.run.hospital
        links = map_links(obj.latitude, obj.longitude)
        return format_html('<table><thead><tr><th>Supplied facility</th><th>Proposed position / source</th></tr></thead>'
            '<tbody><tr><td>{}<br>{}<br>{}, {} {}<br>Source ID: {}</td>'
            '<td>{}, {}<br>{}<br>Source: {} · Relevance: {}<br>'
            '<a href="{}" target="_blank" rel="noopener noreferrer">Inspect in Google Maps</a> · '
            '<a href="{}" target="_blank" rel="noopener noreferrer">Inspect in OpenStreetMap</a></td></tr></tbody></table>'
            '<p><a href="{}">Open exact-location and duplicate-review workspace</a></p>',
            hospital.name, hospital.address, hospital.city, hospital.state, hospital.postal_code, hospital.source_record_id,
            obj.latitude, obj.longitude, obj.label, obj.provider, obj.confidence if obj.confidence is not None else "Not supplied / manual",
            links["google"], links["osm"], reverse("admin:community_care_hospital_location_review", args=[hospital.pk]))

    @admin.display(description="Hospital")
    def hospital_name(self, obj):
        return obj.run.hospital.name

    @admin.display(description="Current hospital revision for action")
    def hospital_revision(self, obj):
        return obj.run.hospital.revision

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        return super().get_actions(request) if can_verify(request.user) else {}

    def decide(self, request, queryset, decision):
        from .location_review import review_location
        if queryset.count() != 1:
            self.message_user(request, "Review exactly one proposal at a time.", messages.ERROR)
            return
        try:
            review_location(request.user, queryset.get().pk, decision, int(request.POST.get("expected_revision", "")),
                request.POST.get("reason", ""), request.POST.get("exact_location_confirmed") == "on",
                request.POST.get("duplicate_review_resolved") == "on", evidence_url=request.POST.get("evidence_url", ""))
        except (ValueError, APIException) as error:
            self.message_user(request, str(error), messages.ERROR)
            return
        self.message_user(request, "Location decision audited. Hospital identity verification is unchanged.")

    @admin.action(description="Approve exact location (independent evidence required)")
    def approve_location(self, request, queryset):
        self.decide(request, queryset, "APPROVE")

    @admin.action(description="Reject proposed location")
    def reject_location(self, request, queryset):
        self.decide(request, queryset, "REJECT")


@admin.register(GeocodingRun)
class GeocodingRunAdmin(DirectoryAdmin):
    list_display = ("id", "hospital", "status", "error_code", "created_at")
    readonly_fields = tuple(field.name for field in GeocodingRun._meta.fields)
    actions = None

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False


@admin.register(HospitalDuplicateReview)
class HospitalDuplicateReviewAdmin(DirectoryAdmin):
    list_display = ("hospital", "status", "related_hospital", "reviewed_by", "reviewed_at", "revision")
    list_filter = ("status",)
    search_fields = ("hospital__name", "hospital__source_record_id")
    readonly_fields = tuple(field.name for field in HospitalDuplicateReview._meta.fields) + ("workspace",)
    actions = None

    @admin.display(description="Make an audited human decision")
    def workspace(self, obj):
        return format_html('<a href="{}">Open hospital review workspace</a>',
                           reverse("admin:community_care_hospital_location_review", args=[obj.hospital_id]))

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False
