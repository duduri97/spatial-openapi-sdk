package io.github.duduri97.spatial.gis;

import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/openapi/v1")
public class SpatialOpenApiController {

    private final SpatialFeatureRepository featureRepository;
    private final CoordinateTransformService transformService;

    public SpatialOpenApiController(
            SpatialFeatureRepository featureRepository,
            CoordinateTransformService transformService) {
        this.featureRepository = featureRepository;
        this.transformService = transformService;
    }

    @GetMapping("/transform")
    public CoordinateTransformService.Point2D transform(
            @RequestParam double x,
            @RequestParam double y,
            @RequestParam(defaultValue = "EPSG:4326") String from,
            @RequestParam(defaultValue = "EPSG:3857") String to) {
        return transformService.transform(x, y, from, to);
    }

    @GetMapping("/layers")
    public List<Map<String, Object>> layers() {
        return featureRepository.countByLayer().stream()
                .map(row -> Map.<String, Object>of(
                        "layerCode", row[0],
                        "featureCount", row[1]
                ))
                .toList();
    }

    @GetMapping("/features/geojson")
    public Map<String, Object> featuresGeoJson(
            @RequestParam(required = false) String layer,
            @RequestParam(defaultValue = "-180") double minLon,
            @RequestParam(defaultValue = "-90") double minLat,
            @RequestParam(defaultValue = "180") double maxLon,
            @RequestParam(defaultValue = "90") double maxLat) {
        String normalizedLayer = (layer == null || layer.isBlank() || "ALL".equalsIgnoreCase(layer))
                ? null : layer.trim().toUpperCase();
        List<SpatialFeature> features = featureRepository.findInBoundingBox(
                normalizedLayer, minLon, minLat, maxLon, maxLat);

        List<Map<String, Object>> geoFeatures = features.stream()
                .map(this::toGeoJsonFeature)
                .toList();

        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("count", geoFeatures.size());
        collection.put("crs", "EPSG:4326");
        collection.put("features", geoFeatures);
        return collection;
    }

    @GetMapping("/features/nearby")
    public Map<String, Object> nearby(
            @RequestParam double lon,
            @RequestParam double lat,
            @RequestParam(defaultValue = "3000") double radiusMeters,
            @RequestParam(required = false) String layer) {
        if (radiusMeters <= 0 || radiusMeters > 50_000) {
            throw new IllegalArgumentException("radiusMeters must be between 1 and 50000");
        }

        // Fast bounding-box pre-filter before exact Haversine distance calculation
        double latDelta = radiusMeters / 111_320.0;
        double lonDelta = radiusMeters / (111_320.0 * Math.max(0.1, Math.cos(Math.toRadians(lat))));
        String normalizedLayer = (layer == null || layer.isBlank() || "ALL".equalsIgnoreCase(layer))
                ? null : layer.trim().toUpperCase();

        List<SpatialFeature> candidates = featureRepository.findInBoundingBox(
                normalizedLayer, lon - lonDelta, lat - latDelta, lon + lonDelta, lat + latDelta);

        List<Map<String, Object>> results = candidates.stream()
                .map(f -> {
                    double dist = transformService.haversineMeters(lon, lat, f.getLongitude(), f.getLatitude());
                    return Map.entry(f, dist);
                })
                .filter(e -> e.getValue() <= radiusMeters)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(e -> {
                    Map<String, Object> item = toGeoJsonFeature(e.getKey());
                    @SuppressWarnings("unchecked")
                    Map<String, Object> props = (Map<String, Object>) item.get("properties");
                    props.put("distanceMeters", Math.round(e.getValue() * 10.0) / 10.0);
                    return item;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "FeatureCollection");
        response.put("center", List.of(lon, lat));
        response.put("radiusMeters", radiusMeters);
        response.put("count", results.size());
        response.put("features", results);
        return response;
    }

    private Map<String, Object> toGeoJsonFeature(SpatialFeature f) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", f.getId());
        properties.put("layerCode", f.getLayerCode());
        properties.put("name", f.getName());
        properties.put("category", f.getCategory());
        properties.put("elevation", f.getElevation());
        properties.put("description", f.getDescription());

        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", List.of(f.getLongitude(), f.getLatitude(), f.getElevation()));

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("id", f.getId());
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        return feature;
    }
}
