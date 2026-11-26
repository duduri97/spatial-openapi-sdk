package io.github.duduri97.spatial.gis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CoordinateTransformServiceTest {

    private final CoordinateTransformService service = new CoordinateTransformService();

    @Test
    void roundTripWgs84ToWebMercatorAndBack() {
        var mercator = service.transform(126.9780, 37.5665, "EPSG:4326", "EPSG:3857");
        assertThat(mercator.crs()).isEqualTo("EPSG:3857");
        assertThat(mercator.x()).isCloseTo(14135126.5, within(10.0));
        assertThat(mercator.y()).isCloseTo(4518376.4, within(10.0));

        var back = service.transform(mercator.x(), mercator.y(), "EPSG:3857", "EPSG:4326");
        assertThat(back.x()).isCloseTo(126.9780, within(0.0001));
        assertThat(back.y()).isCloseTo(37.5665, within(0.0001));
    }

    @Test
    void roundTripWgs84ToEpsg5179AndBack() {
        // Seoul City Hall in EPSG:5179 is approximately (953931, 1952053)
        var utmk = service.transform(126.9780, 37.5665, "EPSG:4326", "EPSG:5179");
        assertThat(utmk.crs()).isEqualTo("EPSG:5179");
        assertThat(utmk.x()).isCloseTo(953931.0, within(50.0));
        assertThat(utmk.y()).isCloseTo(1952053.0, within(50.0));

        var back = service.transform(utmk.x(), utmk.y(), "EPSG:5179", "EPSG:4326");
        assertThat(back.x()).isCloseTo(126.9780, within(0.0001));
        assertThat(back.y()).isCloseTo(37.5665, within(0.0001));
    }

    @Test
    void calculatesHaversineDistanceBetweenSeoulLandmarks() {
        // Seoul City Hall to Gwanghwamun Gate is ~1.05 km
        double dist = service.haversineMeters(126.9780, 37.5665, 126.9769, 37.5759);
        assertThat(dist).isBetween(950.0, 1150.0);
    }

    @Test
    void rejectsUnsupportedCrs() {
        assertThatThrownBy(() -> service.transform(126.9, 37.5, "EPSG:9999", "EPSG:4326"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported CRS");
    }
}
