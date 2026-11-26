package io.github.duduri97.spatial.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ApiUsageRollupRepository extends JpaRepository<ApiUsageRollup, Long> {

    Optional<ApiUsageRollup> findByApiKeyAndEndpointAndUsageDate(String apiKey, String endpoint, LocalDate usageDate);

    @Query("SELECT COALESCE(SUM(r.requestCount), 0) FROM ApiUsageRollup r WHERE r.apiKey = :apiKey AND r.usageDate = :usageDate")
    long sumDailyRequests(@Param("apiKey") String apiKey, @Param("usageDate") LocalDate usageDate);
}
