# MapTiler Cloud geocoding attribution

Geocoding candidates in the Community Care database are supplied by MapTiler
Cloud. Each stored candidate retains the returned attribution, provider feature
reference, retrieval timestamp, relevance and review flags. A locality reference
is a search heuristic, never a confirmed hospital location.

© MapTiler — https://www.maptiler.com/copyright/

© OpenStreetMap contributors — https://www.openstreetmap.org/copyright

Retain the actual response's additional attribution when exporting results or
copying approved coordinates. Nearby API responses include location attribution.
Display applicable provider credits in a future map UI. Do not render stored
provider HTML as trusted markup.

The current Cloud terms permit geocoding-result export and require attribution
for databases built from geocoding. This is not permission to cache map tiles,
redistribute map imagery, or create a substitute geocoding dataset. Account plan,
quota and applicable agreement restrictions still apply. No map tiles are cached.

Terms reviewed 2026-10-26:

- https://www.maptiler.com/terms/cloud/ (sections 6.3 and 6.4)
- https://www.maptiler.com/search/ (permanent geocoding storage FAQ)
- https://docs.maptiler.com/cloud/api/geocoding/ (backend API and attribution)

The Server & Data product terms are separate; this implementation calls the
documented MapTiler Cloud geocoding endpoint only. Feature IDs are provider
references, not stable hospital identifiers or evidence of verification.
