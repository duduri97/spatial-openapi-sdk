# Changelog

All notable changes to `spatial-openapi-sdk` will be documented in this file.

## [1.0.0] - 2026-11-26

### Added
- API Key lifecycle (`PENDING → APPROVED → REVOKED`) and daily quota rate-limiting filter (`ApiKeyAuthFilter`).
- Daily rollup usage metrics (`ApiUsageMetricsService`) reducing verbose per-request log storage overhead.
- Pure-Java coordinate transformation engine supporting `EPSG:4326` (WGS84), `EPSG:3857` (Web Mercator), and `EPSG:5179` (Korea 2000 / UTM-K).
- GeoJSON bounding-box query and two-stage Haversine nearby radius search OpenAPI endpoints.
- Built-in OGC WMS 1.3.0 compatible raster map endpoint (`/geoserver/spatial/wms`) + Docker GeoServer auto-publishing script (`scripts/publish-geoserver.ps1`).
- Lightweight `SpatialMapSDK` JavaScript client, OpenLayers demo page, JUnit 5 + JaCoCo CI pipeline, and Dual-Mode CORS integration with `realtime-3d-twin`.
