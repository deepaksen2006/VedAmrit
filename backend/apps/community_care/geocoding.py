"""Bounded MapTiler Cloud geocoding, with no credentials in errors or storage."""
import hashlib
import json
import math
import re
import socket
import time
from datetime import timedelta
from decimal import Decimal, InvalidOperation
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import HTTPRedirectHandler, Request, build_opener

from django.conf import settings
from django.db import transaction
from django.utils import timezone

from .models import GeocodingRun, LocationCandidate

ATTRIBUTION = '© MapTiler © OpenStreetMap contributors; https://www.maptiler.com/copyright/; https://www.openstreetmap.org/copyright'
SNAPSHOT_FIELDS = ("name", "address", "city", "district", "state", "postal_code")


class GeocodingError(Exception):
    def __init__(self, code):
        self.code = code
        super().__init__(f"Geocoding unavailable: {code}.")


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class MapTilerClient:
    def __init__(self):
        self.key = settings.MAPTILER_API_KEY
        if not self.key:
            raise GeocodingError("MISSING_KEY")
        self.timeout = min(30, max(1, settings.MAPTILER_GEOCODING_TIMEOUT))
        self.retries = min(3, max(0, settings.MAPTILER_GEOCODING_RETRIES))
        self.budget = min(100, max(1, settings.MAPTILER_GEOCODING_REQUEST_BUDGET))
        self.interval = max(.2, settings.MAPTILER_GEOCODING_MIN_INTERVAL)
        self.requests = 0
        self.last_request = None
        self.opener = build_opener(NoRedirect())

    def search(self, query):
        url = "https://api.maptiler.com/geocoding/" + quote(query, safe="") + ".json?" + urlencode(
            {"key": self.key, "country": "in", "language": "en", "limit": 5, "autocomplete": "false"})
        for attempt in range(self.retries + 1):
            if self.requests >= self.budget:
                raise GeocodingError("REQUEST_BUDGET")
            if self.last_request is not None:
                time.sleep(max(0, self.interval - (time.monotonic() - self.last_request)))
            self.requests += 1
            self.last_request = time.monotonic()
            delay = 2 ** attempt
            try:
                with self.opener.open(Request(url, headers={"User-Agent": "VedAmrit/CommunityCareGeocoding"}), timeout=self.timeout) as response:
                    raw = response.read(2_000_001)
                if len(raw) > 2_000_000:
                    raise GeocodingError("RESPONSE_SIZE")
                payload = json.loads(raw)
                if not isinstance(payload, dict) or not isinstance(payload.get("features"), list):
                    raise GeocodingError("INVALID_RESPONSE")
                return payload
            except HTTPError as error:
                code = "RATE_LIMITED" if error.code == 429 else "AUTH_RESTRICTED" if error.code in (401, 403) else "SERVER_ERROR" if error.code >= 500 else "HTTP_ERROR"
                if error.code == 429:
                    try:
                        delay = max(delay, float(error.headers.get("Retry-After", delay)))
                    except (TypeError, ValueError):
                        # Date-valued/unknown Retry-After: defer to an operator rather than retry early.
                        raise GeocodingError("RATE_LIMITED") from None
                if code not in {"RATE_LIMITED", "SERVER_ERROR"} or delay > 30 or attempt == self.retries:
                    raise GeocodingError(code) from None
            except (URLError, TimeoutError, socket.timeout, OSError):
                code = "NETWORK_TIMEOUT"
                if attempt == self.retries:
                    raise GeocodingError(code) from None
            except (ValueError, UnicodeError):
                raise GeocodingError("INVALID_RESPONSE") from None
            time.sleep(delay)
        raise GeocodingError("REQUEST_FAILED")


def snapshot(hospital):
    return {field: getattr(hospital, field) for field in SNAPSHOT_FIELDS}


def query_details(hospital):
    data = snapshot(hospital)
    original = ", ".join(dict.fromkeys(value.strip() for value in data.values() if value.strip())) + ", India"
    query = original
    if len(re.findall(r"\w+", query)) > 20 or len(query) > 256:
        context = list(dict.fromkeys(value.strip() for field, value in data.items() if field not in {"name", "address"} and value.strip())) + ["India"]
        context_words = sum(len(re.findall(r"\w+", value)) for value in context)
        budget = max(2, 20 - context_words)
        name = re.findall(r"\w+", data["name"])[:max(1, budget // 2)]
        address = re.findall(r"\w+", data["address"])[:max(1, budget - len(name))]
        query = ", ".join([" ".join(name), " ".join(address), *context])
    policy = "maptiler-in-en-v1" if query == original else "maptiler-in-en-bounded-v1"
    digest = hashlib.sha256(json.dumps({"snapshot": data, "policy": policy, "limit": 5}, sort_keys=True).encode()).hexdigest()
    return data, query, digest


def distance_km(lat1, lon1, lat2, lon2):
    p1, p2 = math.radians(float(lat1)), math.radians(float(lat2))
    dlat, dlon = p2 - p1, math.radians(float(lon2) - float(lon1))
    a = math.sin(dlat / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlon / 2) ** 2
    return 6371.0088 * 2 * math.asin(math.sqrt(min(1, max(0, a))))


def features(payload):
    result = []
    def clean(value, limit):
        text = str(value or "")
        if settings.MAPTILER_API_KEY:
            text = text.replace(settings.MAPTILER_API_KEY, "[redacted]")
        return text[:limit]
    for feature in payload["features"][:5]:
        try:
            if not isinstance(feature, dict):
                continue
            geometry = feature.get("geometry", {})
            if geometry.get("type") != "Point":
                continue
            raw_lon, raw_lat = geometry["coordinates"][:2]
            lat, lon = Decimal(str(raw_lat)), Decimal(str(raw_lon))
            if not lat.is_finite() or not lon.is_finite() or not -90 <= lat <= 90 or not -180 <= lon <= 180:
                continue
            relevance = feature.get("relevance")
            confidence = float(relevance) if relevance is not None else None
            if confidence is not None and (not math.isfinite(confidence) or not 0 <= confidence <= 1):
                confidence = None
            types = feature.get("place_type", [])
            result.append({"latitude": lat.quantize(Decimal("0.000001")), "longitude": lon.quantize(Decimal("0.000001")),
                "confidence": confidence, "provider_reference": clean(feature.get("id"), 255),
                "label": clean(feature.get("place_name") or feature.get("text"), 1000),
                "feature_types": [clean(t, 80) for t in types if isinstance(t, str)] if isinstance(types, list) else [],
                "attribution": clean(payload.get("attribution"), 5000) or ATTRIBUTION})
        except (KeyError, ValueError, TypeError, AttributeError, InvalidOperation):
            continue
    return result


def geocode_hospital(hospital, client, reference_cache):
    data, query, digest = query_details(hospital)
    with transaction.atomic():
        run, _ = GeocodingRun.objects.get_or_create(hospital=hospital, query_hash=digest, defaults={"query": query, "snapshot": data})
        run = GeocodingRun.objects.select_for_update().get(pk=run.pk)
        if run.status in {"SUCCESS", "NO_RESULTS"}:
            return run, "skipped"
        if run.retry_after and run.retry_after > timezone.now():
            return run, "cooldown"
        try:
            ref_key = (hospital.city, hospital.district, hospital.state)
            if ref_key not in reference_cache:
                prior = GeocodingRun.objects.filter(hospital__city=hospital.city, hospital__district=hospital.district,
                    hospital__state=hospital.state).exclude(reference_context={}).first()
                reference = prior.reference_context if prior else {}
                if not reference:
                    ref_query = ", ".join(dict.fromkeys(value for value in ref_key if value)) + ", India"
                    refs = features(client.search(ref_query))
                    # Reference is a provider-reported locality centroid, NEVER a hospital coordinate.
                    refs = [r for r in refs if any(t in {"municipality", "locality", "place", "city"} for t in r["feature_types"])
                            and hospital.state.casefold() in r["label"].casefold()]
                    if len(refs) == 1:
                        reference = {k: str(v) if isinstance(v, Decimal) else v for k, v in refs[0].items()}
                reference_cache[ref_key] = reference
            run.reference_context = reference_cache[ref_key]
            payload = client.search(query)
            parsed = features(payload)
            if payload["features"] and not parsed:
                raise GeocodingError("INVALID_CANDIDATES")
            for item in parsed:
                flags = ["EXACT_LOCATION_REQUIRES_HUMAN_CONFIRMATION"]
                full_query = ", ".join(dict.fromkeys(value.strip() for value in data.values() if value.strip())) + ", India"
                if query != full_query:
                    flags.append("QUERY_SHORTENED_FOR_PROVIDER")
                if len(parsed) > 1:
                    flags.append("AMBIGUOUS_RESULTS")
                if item["confidence"] is None or item["confidence"] < .85:
                    flags.append("LOW_OR_MISSING_RELEVANCE")
                if not any(t in {"poi", "address", "house"} for t in item["feature_types"]):
                    flags.append("BROAD_LOCATION_NOT_CONFIRMED_FACILITY")
                if hospital.city.casefold() not in item["label"].casefold() or hospital.state.casefold() not in item["label"].casefold():
                    flags.append("LOCALITY_MISMATCH")
                if hospital.import_provenance.get("duplicate_candidates"):
                    flags.append("DUPLICATE_REVIEW_REQUIRED")
                ref = run.reference_context
                distance = distance_km(item["latitude"], item["longitude"], ref["latitude"], ref["longitude"]) if ref else None
                if distance is None:
                    flags.append("DISTANCE_REFERENCE_UNAVAILABLE")
                elif distance > settings.MAPTILER_GEOCODING_DISTANCE_WARNING_KM:
                    flags.append("DISTANT_FROM_LOCALITY_REFERENCE")
                fingerprint = hashlib.sha256(json.dumps({key: str(item[key]) for key in ("latitude", "longitude", "provider_reference")}, sort_keys=True).encode()).hexdigest()
                LocationCandidate.objects.get_or_create(run=run, fingerprint=fingerprint,
                    defaults={**item, "flags": flags, "distance_from_reference_km": distance})
            run.status = "SUCCESS" if parsed else "NO_RESULTS"
            run.error_code = ""
            run.retry_after = None
            run.save()
            return run, "generated"
        except GeocodingError as error:
            run.status, run.error_code = "FAILED", error.code
            run.retry_after = timezone.now() + timedelta(minutes=5)
            run.save()
            return run, "failed"
