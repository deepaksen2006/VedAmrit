from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any

from django.contrib.auth import get_user_model
from django.db import transaction
from rest_framework import authentication, exceptions
from rest_framework_simplejwt.authentication import JWTAuthentication

from apps.authentication.models import FirebaseIdentity


class FirebaseOrJWTAuthentication(authentication.BaseAuthentication):
    """Authenticate Firebase patient tokens and preserve existing doctor JWTs."""

    keyword = "Bearer"

    def __init__(self) -> None:
        self.jwt_authentication = JWTAuthentication()

    def authenticate(self, request):
        header = authentication.get_authorization_header(request).decode("utf-8")
        if not header:
            return None
        parts = header.split()
        if len(parts) != 2 or parts[0] != self.keyword:
            return None
        token = parts[1]

        firebase_failure: Exception | None = None
        try:
            firebase_claims = verify_firebase_id_token(token)
            user = get_or_create_firebase_user(firebase_claims)
            return (user, firebase_claims)
        except Exception as exc:
            firebase_failure = exc

        try:
            jwt_result = self.jwt_authentication.authenticate(request)
            if jwt_result is not None:
                return jwt_result
        except Exception:
            pass

        raise exceptions.AuthenticationFailed(
            "Invalid or expired authentication token."
        ) from firebase_failure


def verify_firebase_id_token(token: str) -> dict[str, Any]:
    ensure_firebase_app()
    from firebase_admin import auth as firebase_auth

    return firebase_auth.verify_id_token(token, check_revoked=True)


def ensure_firebase_app() -> None:
    import firebase_admin
    from firebase_admin import credentials

    if firebase_admin._apps:
        return

    credential_json = os.getenv("FIREBASE_ADMIN_CREDENTIAL_JSON")
    credential_path = os.getenv("FIREBASE_ADMIN_CREDENTIALS")

    if credential_json:
        firebase_admin.initialize_app(credentials.Certificate(json.loads(credential_json)))
        return

    if credential_path:
        firebase_admin.initialize_app(credentials.Certificate(str(Path(credential_path))))
        return

    firebase_admin.initialize_app()


@transaction.atomic
def get_or_create_firebase_user(claims: dict[str, Any]):
    firebase_uid = claims.get("uid") or claims.get("sub")
    if not firebase_uid:
        raise exceptions.AuthenticationFailed("Firebase token is missing a UID.")

    email = (claims.get("email") or "").strip().lower()
    identity = FirebaseIdentity.objects.select_related("user").filter(firebase_uid=firebase_uid).first()
    if identity:
        if email and identity.email != email:
            identity.email = email
            identity.save(update_fields=["email", "updated_at"])
        return identity.user

    User = get_user_model()
    username = f"firebase:{firebase_uid}"
    user = User.objects.create_user(
        username=username[:150],
        email=email,
        password=None,
    )
    FirebaseIdentity.objects.create(user=user, firebase_uid=firebase_uid, email=email)
    return user
