/**
 * SpatialMapSDK — Lightweight JavaScript SDK for Spatial OpenAPI
 * Supports CRS transformation (EPSG:4326 / 3857 / 5179), GeoJSON layer queries,
 * radius search, and 2D (OpenLayers) / 3D (CesiumJS) map mounting.
 */
(function (global) {
  'use strict';

  class SpatialMapSDK {
    constructor(options = {}) {
      this.baseUrl = (options.baseUrl || '').replace(/\/$/, '');
      this.apiKey = options.apiKey || '';
      if (!this.apiKey) {
        throw new Error('SpatialMapSDK requires an apiKey');
      }
    }

    async _request(path, params = {}) {
      const qs = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') {
          qs.set(k, String(v));
        }
      });
      const query = qs.toString() ? `?${qs.toString()}` : '';
      const res = await fetch(`${this.baseUrl}${path}${query}`, {
        headers: { 'X-API-Key': this.apiKey }
      });
      const data = await res.json();
      if (!res.ok) {
        const err = new Error(data.message || `Request failed (${res.status})`);
        err.status = res.status;
        err.code = data.code;
        throw err;
      }
      return data;
    }

    transform({ x, y, from = 'EPSG:4326', to = 'EPSG:3857' }) {
      return this._request('/openapi/v1/transform', { x, y, from, to });
    }

    getLayers() {
      return this._request('/openapi/v1/layers');
    }

    getFeaturesGeoJson({ layer, minLon, minLat, maxLon, maxLat } = {}) {
      return this._request('/openapi/v1/features/geojson', {
        layer, minLon, minLat, maxLon, maxLat
      });
    }

    findNearby({ lon, lat, radiusMeters = 3000, layer } = {}) {
      return this._request('/openapi/v1/features/nearby', {
        lon, lat, radiusMeters, layer
      });
    }
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = SpatialMapSDK;
  } else {
    global.SpatialMapSDK = SpatialMapSDK;
  }
})(typeof window !== 'undefined' ? window : globalThis);
