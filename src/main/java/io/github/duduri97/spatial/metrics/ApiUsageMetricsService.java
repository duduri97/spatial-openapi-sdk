package io.github.duduri97.spatial.metrics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ApiUsageMetricsService {

    private final ApiUsageRollupRepository repository;

    public ApiUsageMetricsService(ApiUsageRollupRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized void record(String apiKey, String endpoint, long latencyMs, boolean isError) {
        LocalDate today = LocalDate.now();
        ApiUsageRollup rollup = repository.findByApiKeyAndEndpointAndUsageDate(apiKey, endpoint, today)
                .orElseGet(() -> new ApiUsageRollup(apiKey, endpoint, today));
        rollup.add(latencyMs, isError);
        repository.save(rollup);
    }

    @Transactional(readOnly = true)
    public long getTodayRequestCount(String apiKey) {
        return repository.sumDailyRequests(apiKey, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public MetricsSummary getSummary() {
        List<ApiUsageRollup> rollups = repository.findAll();
        long totalRequests = rollups.stream().mapToLong(ApiUsageRollup::getRequestCount).sum();
        long rollupRows = rollups.size();
        double rowReductionPercent = totalRequests == 0
                ? 0.0
                : Math.round(((totalRequests - rollupRows) * 1000.0) / totalRequests) / 10.0;
        return new MetricsSummary(totalRequests, rollupRows, Math.max(0.0, rowReductionPercent), rollups);
    }

    public record MetricsSummary(
            long totalRequests,
            long storedRollupRows,
            double rowReductionPercent,
            List<ApiUsageRollup> rollups
    ) {}
}
