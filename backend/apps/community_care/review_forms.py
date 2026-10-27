from decimal import Decimal
from urllib.parse import urlencode
from uuid import uuid4

from django import forms

from .location_review import validate_evidence_url
from .models import Hospital, HospitalDuplicateReview, LocationCandidate


def map_links(latitude, longitude):
    lat, lon = format(Decimal(str(latitude)), "f"), format(Decimal(str(longitude)), "f")
    return {"google": "https://www.google.com/maps/search/?" + urlencode({"api": "1", "query": f"{lat},{lon}"}),
            "osm": f"https://www.openstreetmap.org/?mlat={lat}&mlon={lon}#map=18/{lat}/{lon}"}


class EvidenceForm(forms.Form):
    def clean_evidence_url(self):
        from rest_framework.exceptions import ValidationError
        try:
            return validate_evidence_url(self.cleaned_data.get("evidence_url"), required=True)
        except ValidationError:
            raise forms.ValidationError("Use a public HTTP(S) facility evidence URL without credentials or API tokens.") from None


class CandidateChoice(forms.ModelChoiceField):
    def label_from_instance(self, obj):
        return f"#{obj.pk}: {obj.latitude}, {obj.longitude} — {obj.provider} — {obj.label[:100]}"


class HospitalChoice(forms.ModelChoiceField):
    def label_from_instance(self, obj):
        return f"{obj.source_record_id or '#' + str(obj.pk)} — {obj.name} — {obj.address[:100]}"


class ManualLocationForm(EvidenceForm):
    expected_revision = forms.IntegerField(min_value=1, widget=forms.HiddenInput)
    submission_id = forms.UUIDField(initial=uuid4, widget=forms.HiddenInput)
    latitude = forms.DecimalField(max_digits=9, decimal_places=6, min_value=Decimal("-90"), max_value=Decimal("90"))
    longitude = forms.DecimalField(max_digits=10, decimal_places=6, min_value=Decimal("-180"), max_value=Decimal("180"))
    evidence_url = forms.URLField(max_length=1000, label="Independent facility-location evidence URL")
    reason = forms.CharField(min_length=20, max_length=2000, widget=forms.Textarea(attrs={"rows": 4}),
                             help_text="Describe how the exact facility/entrance position was independently checked. A pin alone is not identity evidence.")
    based_on_candidate = CandidateChoice(queryset=LocationCandidate.objects.none(), required=False,
                                                label="Original proposal being corrected (optional)")
    exact_location_confirmed = forms.BooleanField(label="I independently confirmed this exact facility position")
    duplicate_review_resolved = forms.BooleanField(required=False, label="I checked the current recorded duplicate review (required for flagged facilities)")
    confirm_save = forms.BooleanField(required=False, label="After preview, I authorize saving these confirmed coordinates")

    def __init__(self, *args, hospital, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["based_on_candidate"].queryset = LocationCandidate.objects.filter(run__hospital=hospital)


class DuplicateReviewForm(EvidenceForm):
    expected_revision = forms.IntegerField(min_value=1, widget=forms.HiddenInput)
    expected_review_revision = forms.IntegerField(min_value=1, widget=forms.HiddenInput)
    status = forms.ChoiceField(choices=HospitalDuplicateReview.Status.choices)
    related_hospital = HospitalChoice(queryset=Hospital.objects.none(), required=False,
        label="Related retained hospital (required only for confirmed duplicate)")
    evidence_url = forms.URLField(max_length=1000, label="Independent identity evidence URL")
    reason = forms.CharField(min_length=20, max_length=2000, widget=forms.Textarea(attrs={"rows": 4}))
    human_confirmed = forms.BooleanField(label="I independently reviewed these facility identities; this action does not merge or delete them")

    def __init__(self, *args, hospital, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["related_hospital"].queryset = Hospital.objects.exclude(pk=hospital.pk).order_by("name", "id")
