"""Read-only live DB snapshot/API smoke; prints counts/digests, never credentials.

Run with backend virtualenv Python from the project root. No migrations or writes.
"""
import hashlib
import json
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "backend"))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "vedaahar_backend.settings")
import django
django.setup()

from django.apps import apps
from django.db import connection, transaction
from django.test import override_settings
from rest_framework.test import APIClient
from apps.community_care.models import Hospital

with transaction.atomic():
    if connection.vendor == "postgresql":
        with connection.cursor() as cursor:
            cursor.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY")
    tables = {}
    for model in apps.get_models():
        rows = list(model.objects.order_by("pk").values())
        digest = hashlib.sha256(json.dumps(rows, sort_keys=True, default=str, ensure_ascii=True).encode()).hexdigest()
        tables[model._meta.label] = {"count": len(rows), "sha256": digest}
    hospitals = list(Hospital.objects.order_by("source_record_id", "pk").values(
        "id", "source_record_id", "verification_status", "location_status", "import_review_status", "booking_enabled"))
    with override_settings(ALLOWED_HOSTS=["testserver"]):
        client = APIClient()
        endpoints = {}
        for url in ("/api/care/hospitals/", "/api/care/hospitals/?search=Udaipur",
                    "/api/care/hospitals/nearby/?latitude=24.5854&longitude=73.7125&radius_km=20"):
            response = client.get(url)
            endpoints[url] = {"status": response.status_code, "count": response.data.get("count")}
        for hospital in hospitals:
            endpoints[f"hospital:{hospital['id']}"] = {"status": client.get(f"/api/care/hospitals/{hospital['id']}/").status_code}
    print(json.dumps({"database_vendor": connection.vendor, "tables": tables, "hospitals": hospitals, "endpoints": endpoints}, indent=2))
