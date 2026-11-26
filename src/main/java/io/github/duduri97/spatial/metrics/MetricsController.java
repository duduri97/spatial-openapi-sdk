package io.github.duduri97.spatial.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final ApiUsageMetricsService metricsService;

    public MetricsController(ApiUsageMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/summary")
    public ApiUsageMetricsService.MetricsSummary summary() {
        return metricsService.getSummary();
    }
}
