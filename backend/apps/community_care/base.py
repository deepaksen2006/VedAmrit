from django.apps import apps
from django.conf import settings
from django.db import models, router, transaction


class VerificationStatus(models.TextChoices):
    DRAFT = "DRAFT", "Draft"
    PENDING = "PENDING", "Pending"
    VERIFIED = "VERIFIED", "Verified"
    REJECTED = "REJECTED", "Rejected"
    SUSPENDED = "SUSPENDED", "Suspended"


class VerifiedRecord(models.Model):
    """Shared lifecycle; management services supply the authenticated audit context.

    Model-level invalidation also covers the existing doctor self-update route.
    Bulk updates bypass save() and must never be used for verification/identity edits.
    """

    verification_status = models.CharField(max_length=12, choices=VerificationStatus.choices, default=VerificationStatus.DRAFT)
    is_active = models.BooleanField(default=True)
    created_by = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL,
                                   related_name="created_%(app_label)s_%(class)s_records")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    revision = models.PositiveIntegerField(default=1, editable=False)
    critical_fields = ()

    class Meta:
        abstract = True

    def save(self, *args, **kwargs):
        using = kwargs.get("using") or router.db_for_write(type(self), instance=self)
        fields = kwargs.get("update_fields")
        if fields is not None and not fields:
            return
        fields = set(fields) if fields is not None else None
        with transaction.atomic(using=using):
            previous = type(self).objects.using(using).select_for_update().filter(pk=self.pk).first() if self.pk else None
            if previous and fields is not None:
                for name in ("verification_status", "is_verified"):
                    if hasattr(self, name) and name not in fields:
                        setattr(self, name, getattr(previous, name))
            # Preserve the legacy is_verified contract without creating a second identity.
            if hasattr(self, "is_verified"):
                if previous is None and self.is_verified and self.verification_status == VerificationStatus.DRAFT:
                    self.verification_status = VerificationStatus.VERIFIED
                elif previous and self.verification_status == previous.verification_status and self.is_verified != previous.is_verified:
                    self.verification_status = VerificationStatus.VERIFIED if self.is_verified else VerificationStatus.PENDING
                self.is_verified = self.verification_status == VerificationStatus.VERIFIED
            changed = [] if previous is None else [field.name for field in self._meta.concrete_fields
                if not field.primary_key and field.name not in ("created_at", "updated_at", "revision")
                and (fields is None or field.name in fields or field.attname in fields)
                and getattr(previous, field.attname) != getattr(self, field.attname)]
            invalidated = bool(previous and previous.verification_status == VerificationStatus.VERIFIED
                               and set(changed).intersection(self.critical_fields))
            if invalidated:
                self.verification_status = VerificationStatus.PENDING
                if hasattr(self, "is_verified"):
                    self.is_verified = False
            if previous:
                self.revision = previous.revision + 1
            if fields is not None:
                fields.update(("revision", "updated_at", "verification_status"))
                if hasattr(self, "is_verified"):
                    fields.add("is_verified")
                kwargs["update_fields"] = fields
            super().save(*args, **kwargs)
            if previous is None or changed or invalidated:
                audit = apps.get_model("community_care", "VerificationAudit")
                audit.objects.using(using).create(
                    actor=getattr(self, "_care_actor", None), record_type=self.care_record_type, record_id=self.pk,
                    action="REVERIFY_REQUIRED" if invalidated else getattr(self, "_care_action", "CREATE" if previous is None else "EDIT"),
                    previous_status=previous.verification_status if previous else "",
                    new_status=self.verification_status,
                    reason=getattr(self, "_care_reason", "") or ("Critical published fields changed; re-verification required." if invalidated else ""),
                    details={**getattr(self, "_care_audit_details", {}), "changed_fields": changed, "revision": self.revision},
                )
        for name in ("_care_actor", "_care_action", "_care_reason", "_care_audit_details"):
            self.__dict__.pop(name, None)
