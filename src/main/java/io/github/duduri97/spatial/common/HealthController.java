package io.github.duduri97.spatial.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "spatial-openapi-sdk",
                "status", "UP",
                "supportedCrs", new String[]{"EPSG:4326", "EPSG:3857", "EPSG:5179"}
        );
    }
}
