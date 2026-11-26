package io.github.duduri97.spatial.gis;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Built-in OGC WMS 1.1.1 / 1.3.0 compatible 2D raster map endpoint (/geoserver/spatial/wms).
 * Works as a zero-Docker local drop-in for GeoServer WMS (GetMap image/png, GetCapabilities, GetFeatureInfo),
 * while production/Docker deployments can switch directly to GeoServer (:8600/geoserver/spatial/wms).
 */
@RestController
@RequestMapping("/geoserver/spatial/wms")
public class WmsImageController {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final SpatialFeatureRepository featureRepository;
    private final CoordinateTransformService transformService;

    public WmsImageController(
            SpatialFeatureRepository featureRepository,
            CoordinateTransformService transformService) {
        this.featureRepository = featureRepository;
        this.transformService = transformService;
    }

    @GetMapping
    public ResponseEntity<?> handleWms(@RequestParam Map<String, String> params) throws IOException {
        String request = getParam(params, "REQUEST", "GetMap");
        if ("GetCapabilities".equalsIgnoreCase(request)) {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <WMS_Capabilities version="1.3.0">
                      <Service><Name>WMS</Name><Title>Spatial OpenAPI Built-in GeoServer WMS</Title></Service>
                      <Capability>
                        <Layer>
                          <Title>Spatial Layers</Title>
                          <CRS>EPSG:3857</CRS>
                          <CRS>EPSG:4326</CRS>
                          <Layer queryable="1">
                            <Name>spatial:spatial_features</Name>
                            <Title>Spatial Facilities &amp; Sensors</Title>
                          </Layer>
                        </Layer>
                      </Capability>
                    </WMS_Capabilities>
                    """;
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        }

        if ("GetFeatureInfo".equalsIgnoreCase(request)) {
            return handleGetFeatureInfo(params);
        }

        return handleGetMap(params);
    }

    private ResponseEntity<byte[]> handleGetMap(Map<String, String> params) throws IOException {
        int width = clamp(parseInt(getParam(params, "WIDTH", "512"), 512), 64, 2048);
        int height = clamp(parseInt(getParam(params, "HEIGHT", "512"), 512), 64, 2048);
        String crs = getParam(params, "CRS", getParam(params, "SRS", "EPSG:3857"));
        String bboxStr = getParam(params, "BBOX", "14120000,4505000,14155000,4530000");
        String layers = getParam(params, "LAYERS", "spatial:spatial_features");
        boolean transparent = "true".equalsIgnoreCase(getParam(params, "TRANSPARENT", "true"));

        double[] bbox = parseBboxToWgs84(bboxStr, crs);
        double minLon = bbox[0], minLat = bbox[1], maxLon = bbox[2], maxLat = bbox[3];

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (!transparent) {
                g.setColor(new Color(15, 23, 42));
                g.fillRect(0, 0, width, height);
            }

            // Draw subtle WMS spatial zone grid & Twin Cleanroom Zone boundary around Seoul City Hall
            drawTwinZoneOverlay(g, width, height, minLon, minLat, maxLon, maxLat);

            String layerFilter = extractLayerFilter(layers);
            List<SpatialFeature> features = featureRepository.findInBoundingBox(
                    layerFilter, minLon - 0.02, minLat - 0.02, maxLon + 0.02, maxLat + 0.02);

            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            for (SpatialFeature f : features) {
                int px = (int) Math.round(((f.getLongitude() - minLon) / (maxLon - minLon)) * width);
                int py = (int) Math.round(((maxLat - f.getLatitude()) / (maxLat - minLat)) * height);

                Color fill = colorForLayer(f.getLayerCode());
                // Outer halo
                g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 70));
                g.fillOval(px - 12, py - 12, 24, 24);

                // Marker core
                g.setColor(fill);
                g.fillOval(px - 6, py - 6, 12, 12);
                g.setColor(new Color(15, 23, 42));
                g.setStroke(new BasicStroke(2f));
                g.drawOval(px - 6, py - 6, 12, 12);

                // Label
                String label = f.getName();
                g.setColor(new Color(15, 23, 42, 210));
                g.fillRoundRect(px + 8, py - 9, g.getFontMetrics().stringWidth(label) + 8, 16, 6, 6);
                g.setColor(new Color(248, 250, 252));
                g.drawString(label, px + 12, py + 3);
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(MediaType.IMAGE_PNG)
                .body(baos.toByteArray());
    }

    private ResponseEntity<Map<String, Object>> handleGetFeatureInfo(Map<String, String> params) {
        int width = clamp(parseInt(getParam(params, "WIDTH", "512"), 512), 1, 4096);
        int height = clamp(parseInt(getParam(params, "HEIGHT", "512"), 512), 1, 4096);
        int x = parseInt(getParam(params, "I", getParam(params, "X", "256")), 256);
        int y = parseInt(getParam(params, "J", getParam(params, "Y", "256")), 256);
        String crs = getParam(params, "CRS", getParam(params, "SRS", "EPSG:3857"));
        String bboxStr = getParam(params, "BBOX", "14120000,4505000,14155000,4530000");

        double[] bbox = parseBboxToWgs84(bboxStr, crs);
        double lon = bbox[0] + ((double) x / width) * (bbox[2] - bbox[0]);
        double lat = bbox[3] - ((double) y / height) * (bbox[3] - bbox[1]);

        List<SpatialFeature> all = featureRepository.findAll();
        SpatialFeature nearest = null;
        double minMeters = Double.MAX_VALUE;
        for (SpatialFeature f : all) {
            double d = transformService.haversineMeters(lon, lat, f.getLongitude(), f.getLatitude());
            if (d < minMeters) {
                minMeters = d;
                nearest = f;
            }
        }

        if (nearest == null || minMeters > 2500) {
            return ResponseEntity.ok(Map.of("type", "FeatureCollection", "features", List.of()));
        }
        return ResponseEntity.ok(Map.of(
                "type", "FeatureCollection",
                "features", List.of(Map.of(
                        "type", "Feature",
                        "id", nearest.getId(),
                        "properties", Map.of(
                                "name", nearest.getName(),
                                "layerCode", nearest.getLayerCode(),
                                "category", nearest.getCategory(),
                                "distanceMeters", Math.round(minMeters * 10.0) / 10.0
                        )
                ))
        ));
    }

    private void drawTwinZoneOverlay(Graphics2D g, int width, int height,
                                     double minLon, double minLat, double maxLon, double maxLat) {
        // Highlight Digital Twin Cleanroom Zone around Seoul City Hall (126.971..126.985, 37.561..37.572)
        int x1 = (int) Math.round(((126.971 - minLon) / (maxLon - minLon)) * width);
        int y1 = (int) Math.round(((maxLat - 37.572) / (maxLat - minLat)) * height);
        int x2 = (int) Math.round(((126.985 - minLon) / (maxLon - minLon)) * width);
        int y2 = (int) Math.round(((maxLat - 37.561) / (maxLat - minLat)) * height);

        int rx = Math.min(x1, x2), ry = Math.min(y1, y2);
        int rw = Math.abs(x2 - x1), rh = Math.abs(y2 - y1);
        if (rw > 4 && rh > 4) {
            g.setColor(new Color(56, 189, 248, 36));
            g.fillRect(rx, ry, rw, rh);
            g.setColor(new Color(56, 189, 248, 180));
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6f, 4f}, 0));
            g.drawRect(rx, ry, rw, rh);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("3D TWIN SENSOR ZONE (WMS)", rx + 6, ry + 15);
        }
    }

    private double[] parseBboxToWgs84(String bboxStr, String crs) {
        String[] parts = bboxStr.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid BBOX: " + bboxStr);
        }
        double minX = Double.parseDouble(parts[0].trim());
        double minY = Double.parseDouble(parts[1].trim());
        double maxX = Double.parseDouble(parts[2].trim());
        double maxY = Double.parseDouble(parts[3].trim());

        if ("EPSG:3857".equalsIgnoreCase(crs) || "3857".equals(crs)) {
            var p1 = transformService.transform(minX, minY, "EPSG:3857", "EPSG:4326");
            var p2 = transformService.transform(maxX, maxY, "EPSG:3857", "EPSG:4326");
            return new double[]{p1.x(), p1.y(), p2.x(), p2.y()};
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    private String extractLayerFilter(String layers) {
        if (layers == null || layers.isBlank() || layers.contains("spatial_features")) {
            return null;
        }
        String clean = layers.contains(":") ? layers.substring(layers.indexOf(':') + 1) : layers;
        return clean.trim().toUpperCase();
    }

    private Color colorForLayer(String layerCode) {
        return switch (layerCode == null ? "" : layerCode) {
            case "SENSOR" -> new Color(239, 68, 68);
            case "PARK" -> new Color(52, 211, 153);
            case "TRANSIT" -> new Color(251, 191, 36);
            default -> new Color(56, 189, 248);
        };
    }

    private String getParam(Map<String, String> params, String key, String defaultValue) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null && !entry.getValue().isBlank()) {
                return entry.getValue();
            }
        }
        return defaultValue;
    }

    private int parseInt(String val, int fallback) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return fallback; }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
