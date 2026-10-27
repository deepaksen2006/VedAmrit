from math import isfinite

from django.db.models import F, FloatField, Value, ExpressionWrapper, Q
from django.db.models.functions import ASin, Cos, Least, Greatest, Power, Radians, Sin, Sqrt
from rest_framework.exceptions import ValidationError


def nearby_query(queryset, params):
    if params.get("search"):
        term = params["search"][:200]
        queryset = queryset.filter(Q(name__icontains=term) | Q(city__icontains=term))
    values = {}
    for name, lower, upper in (("latitude", -90, 90), ("longitude", -180, 180), ("radius_km", .01, 100)):
        try:
            raw = params.get(name, "20" if name == "radius_km" else None)
            value = float(raw)
            if not isfinite(value) or not lower <= value <= upper:
                raise ValueError()
        except (ValueError, TypeError):
            raise ValidationError({name: f"Use a finite number between {lower} and {upper}."})
        values[name] = value
    queryset = queryset.filter(location_status="APPROVED", approved_location__status="APPROVED",
        approved_location__run__hospital_id=F("pk"), latitude=F("approved_location__latitude"), longitude=F("approved_location__longitude"),
        latitude__isnull=False, longitude__isnull=False)
    lat, lon = Value(values["latitude"], FloatField()), Value(values["longitude"], FloatField())
    # Haversine on the database, with clamping for identical/antipodal floating-point cases.
    a = Power(Sin((Radians(F("latitude")) - Radians(lat)) / 2), 2) + Cos(Radians(lat)) * Cos(Radians(F("latitude"))) * Power(Sin((Radians(F("longitude")) - Radians(lon)) / 2), 2)
    distance = ExpressionWrapper(12742.0176 * ASin(Sqrt(Least(Value(1.0), Greatest(Value(0.0), a)))), output_field=FloatField())
    return queryset.annotate(distance_km=distance).filter(distance_km__lte=values["radius_km"]).order_by("distance_km", "id")
