package io.github.duduri97.spatial.gis;

import org.springframework.stereotype.Service;

/**
 * Pure-Java coordinate transformation service supporting:
 * - EPSG:4326 (WGS84 Geographic lon/lat)
 * - EPSG:3857 (Web Mercator)
 * - EPSG:5179 (Korea 2000 / Unified CS — Transverse Mercator on GRS80)
 */
@Service
public class CoordinateTransformService {

    private static final double WGS84_R = 6378137.0;

    // GRS80 ellipsoid parameters for EPSG:5179
    private static final double GRS80_A = 6378137.0;
    private static final double GRS80_F = 1.0 / 298.257222101;
    private static final double GRS80_E2 = 2 * GRS80_F - GRS80_F * GRS80_F;
    private static final double GRS80_EP2 = GRS80_E2 / (1.0 - GRS80_E2);

    // EPSG:5179 projection constants (UTM-K)
    private static final double K0_5179 = 0.9996;
    private static final double LAT0_5179 = Math.toRadians(38.0);
    private static final double LON0_5179 = Math.toRadians(127.5);
    private static final double FALSE_EASTING_5179 = 1000000.0;
    private static final double FALSE_NORTHING_5179 = 2000000.0;

    public record Point2D(double x, double y, String crs) {}

    public Point2D transform(double x, double y, String fromCrs, String toCrs) {
        String from = normalizeCrs(fromCrs);
        String to = normalizeCrs(toCrs);
        if (from.equals(to)) {
            return new Point2D(x, y, to);
        }

        double[] wgs84 = toWgs84(x, y, from);
        double[] target = fromWgs84(wgs84[0], wgs84[1], to);
        return new Point2D(round6(target[0]), round6(target[1]), to);
    }

    public double haversineMeters(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2.0 * 6371000.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    private String normalizeCrs(String crs) {
        if (crs == null) {
            throw new IllegalArgumentException("CRS must not be null");
        }
        String normalized = crs.trim().toUpperCase();
        return switch (normalized) {
            case "EPSG:4326", "4326", "WGS84" -> "EPSG:4326";
            case "EPSG:3857", "3857" -> "EPSG:3857";
            case "EPSG:5179", "5179", "UTM-K" -> "EPSG:5179";
            default -> throw new IllegalArgumentException("Unsupported CRS: " + crs + " (supported: EPSG:4326, EPSG:3857, EPSG:5179)");
        };
    }

    private double[] toWgs84(double x, double y, String from) {
        return switch (from) {
            case "EPSG:4326" -> new double[]{x, y};
            case "EPSG:3857" -> webMercatorToWgs84(x, y);
            case "EPSG:5179" -> epsg5179ToWgs84(x, y);
            default -> throw new IllegalArgumentException("Unsupported CRS: " + from);
        };
    }

    private double[] fromWgs84(double lon, double lat, String to) {
        return switch (to) {
            case "EPSG:4326" -> new double[]{lon, lat};
            case "EPSG:3857" -> wgs84ToWebMercator(lon, lat);
            case "EPSG:5179" -> wgs84ToEpsg5179(lon, lat);
            default -> throw new IllegalArgumentException("Unsupported CRS: " + to);
        };
    }

    private double[] wgs84ToWebMercator(double lon, double lat) {
        double x = WGS84_R * Math.toRadians(lon);
        double y = WGS84_R * Math.log(Math.tan(Math.PI / 4.0 + Math.toRadians(lat) / 2.0));
        return new double[]{x, y};
    }

    private double[] webMercatorToWgs84(double x, double y) {
        double lon = Math.toDegrees(x / WGS84_R);
        double lat = Math.toDegrees(2.0 * Math.atan(Math.exp(y / WGS84_R)) - Math.PI / 2.0);
        return new double[]{lon, lat};
    }

    private double[] wgs84ToEpsg5179(double lonDeg, double latDeg) {
        double phi = Math.toRadians(latDeg);
        double lam = Math.toRadians(lonDeg);
        double sinPhi = Math.sin(phi);
        double cosPhi = Math.cos(phi);
        double tanPhi = Math.tan(phi);

        double n = GRS80_A / Math.sqrt(1.0 - GRS80_E2 * sinPhi * sinPhi);
        double t = tanPhi * tanPhi;
        double c = GRS80_EP2 * cosPhi * cosPhi;
        double a = cosPhi * (lam - LON0_5179);

        double m = meridionalArc(phi);
        double m0 = meridionalArc(LAT0_5179);

        double x = FALSE_EASTING_5179 + K0_5179 * n * (
                a
                + (1.0 - t + c) * Math.pow(a, 3) / 6.0
                + (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * GRS80_EP2) * Math.pow(a, 5) / 120.0
        );
        double y = FALSE_NORTHING_5179 + K0_5179 * (
                m - m0 + n * tanPhi * (
                        a * a / 2.0
                        + (5.0 - t + 9.0 * c + 4.0 * c * c) * Math.pow(a, 4) / 24.0
                        + (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * GRS80_EP2) * Math.pow(a, 6) / 720.0
                )
        );
        return new double[]{x, y};
    }

    private double[] epsg5179ToWgs84(double x, double y) {
        double m0 = meridionalArc(LAT0_5179);
        double m = m0 + (y - FALSE_NORTHING_5179) / K0_5179;
        double mu = m / (GRS80_A * (1.0 - GRS80_E2 / 4.0 - 3.0 * GRS80_E2 * GRS80_E2 / 64.0
                - 5.0 * Math.pow(GRS80_E2, 3) / 256.0));

        double e1 = (1.0 - Math.sqrt(1.0 - GRS80_E2)) / (1.0 + Math.sqrt(1.0 - GRS80_E2));
        double phi1 = mu
                + (3.0 * e1 / 2.0 - 27.0 * Math.pow(e1, 3) / 32.0) * Math.sin(2.0 * mu)
                + (21.0 * e1 * e1 / 16.0 - 55.0 * Math.pow(e1, 4) / 32.0) * Math.sin(4.0 * mu)
                + (151.0 * Math.pow(e1, 3) / 96.0) * Math.sin(6.0 * mu)
                + (1097.0 * Math.pow(e1, 4) / 512.0) * Math.sin(8.0 * mu);

        double sinPhi1 = Math.sin(phi1);
        double cosPhi1 = Math.cos(phi1);
        double tanPhi1 = Math.tan(phi1);

        double n1 = GRS80_A / Math.sqrt(1.0 - GRS80_E2 * sinPhi1 * sinPhi1);
        double t1 = tanPhi1 * tanPhi1;
        double c1 = GRS80_EP2 * cosPhi1 * cosPhi1;
        double r1 = GRS80_A * (1.0 - GRS80_E2) / Math.pow(1.0 - GRS80_E2 * sinPhi1 * sinPhi1, 1.5);
        double d = (x - FALSE_EASTING_5179) / (n1 * K0_5179);

        double lat = phi1 - (n1 * tanPhi1 / r1) * (
                d * d / 2.0
                - (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * GRS80_EP2) * Math.pow(d, 4) / 24.0
                + (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 252.0 * GRS80_EP2 - 3.0 * c1 * c1) * Math.pow(d, 6) / 720.0
        );
        double lon = LON0_5179 + (
                d
                - (1.0 + 2.0 * t1 + c1) * Math.pow(d, 3) / 6.0
                + (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * GRS80_EP2 + 24.0 * t1 * t1) * Math.pow(d, 5) / 120.0
        ) / cosPhi1;

        return new double[]{Math.toDegrees(lon), Math.toDegrees(lat)};
    }

    private double meridionalArc(double phi) {
        return GRS80_A * (
                (1.0 - GRS80_E2 / 4.0 - 3.0 * GRS80_E2 * GRS80_E2 / 64.0 - 5.0 * Math.pow(GRS80_E2, 3) / 256.0) * phi
                - (3.0 * GRS80_E2 / 8.0 + 3.0 * GRS80_E2 * GRS80_E2 / 32.0 + 45.0 * Math.pow(GRS80_E2, 3) / 1024.0) * Math.sin(2.0 * phi)
                + (15.0 * GRS80_E2 * GRS80_E2 / 256.0 + 45.0 * Math.pow(GRS80_E2, 3) / 1024.0) * Math.sin(4.0 * phi)
                - (35.0 * Math.pow(GRS80_E2, 3) / 3072.0) * Math.sin(6.0 * phi)
        );
    }

    private double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }
}
