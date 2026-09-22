from __future__ import annotations

from django.conf import settings
from django.db import models


class FirebaseIdentity(models.Model):
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="firebase_identity",
    )
    firebase_uid = models.CharField(max_length=128, unique=True)
    email = models.EmailField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        indexes = [
            models.Index(fields=["firebase_uid"]),
        ]

    def __str__(self) -> str:
        return self.firebase_uid
