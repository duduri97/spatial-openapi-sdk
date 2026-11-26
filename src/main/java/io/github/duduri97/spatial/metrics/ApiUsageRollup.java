package io.github.duduri97.spatial.metrics;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Aggregated daily usage rollup per (apiKey, endpoint, usageDate).
 * Avoids unbounded per-request detail log growth by rolling up counts and latency.
 */
@Entity
@Table(name = "api_usage_rollups", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rollup_key_endpoint_date", columnNames = {"apiKey", "endpoint", "usageDate"})
})
public class ApiUsageRollup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String apiKey;

    @Column(nullable = false, length = 120)
    private String endpoint;

    @Column(nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private long requestCount;

    @Column(nullable = false)
    private long errorCount;

    @Column(nullable = false)
    private long totalLatencyMs;

    protected ApiUsageRollup() {
    }

    public ApiUsageRollup(String apiKey, String endpoint, LocalDate usageDate) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.usageDate = usageDate;
    }

    public void add(long latencyMs, boolean isError) {
        this.requestCount++;
        if (isError) {
            this.errorCount++;
        }
        this.totalLatencyMs += latencyMs;
    }

    public Long getId() { return id; }
    public String getApiKey() { return apiKey; }
    public String getEndpoint() { return endpoint; }
    public LocalDate getUsageDate() { return usageDate; }
    public long getRequestCount() { return requestCount; }
    public long getErrorCount() { return errorCount; }
    public long getTotalLatencyMs() { return totalLatencyMs; }
    public double getAvgLatencyMs() {
        return requestCount == 0 ? 0.0 : Math.round((totalLatencyMs * 100.0) / requestCount) / 100.0;
    }
}
