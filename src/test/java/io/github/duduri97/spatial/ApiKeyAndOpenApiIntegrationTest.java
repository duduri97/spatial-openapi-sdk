package io.github.duduri97.spatial;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyAndOpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fullApiKeyLifecycleOpenApiQuotaAndRollupMetrics() throws Exception {
        // 1. Missing key -> 401
        mockMvc.perform(get("/openapi/v1/layers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("API_KEY_MISSING"));

        // 2. Apply for a new API key with daily quota = 3
        String applyRes = mockMvc.perform(post("/api/v1/keys/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationName":"MapClient","ownerName":"tester","dailyQuota":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(applyRes);
        long keyId = created.get("id").asLong();
        String keyValue = created.get("keyValue").asText();

        // 3. Pending key -> 403
        mockMvc.perform(get("/openapi/v1/layers").header("X-API-Key", keyValue))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("API_KEY_NOT_APPROVED"));

        // 4. Approve key -> 200
        mockMvc.perform(post("/api/v1/keys/{id}/approve", keyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 5. Call OpenAPI endpoints (3 calls = quota limit)
        mockMvc.perform(get("/openapi/v1/features/geojson")
                        .param("layer", "LANDMARK")
                        .header("X-API-Key", keyValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.count").value(5));

        mockMvc.perform(get("/openapi/v1/features/nearby")
                        .param("lon", "126.9780")
                        .param("lat", "37.5665")
                        .param("radiusMeters", "2500")
                        .header("X-API-Key", keyValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features[0].properties.name").value("Seoul City Hall"));

        mockMvc.perform(get("/openapi/v1/transform")
                        .param("x", "126.9780")
                        .param("y", "37.5665")
                        .param("from", "EPSG:4326")
                        .param("to", "EPSG:5179")
                        .header("X-API-Key", keyValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crs").value("EPSG:5179"));

        // 6. 4th call exceeds daily quota -> 429
        mockMvc.perform(get("/openapi/v1/layers").header("X-API-Key", keyValue))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("DAILY_QUOTA_EXCEEDED"));

        // 7. Revoke key -> 403
        mockMvc.perform(post("/api/v1/keys/{id}/revoke", keyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        mockMvc.perform(get("/openapi/v1/layers").header("X-API-Key", keyValue))
                .andExpect(status().isForbidden());

        // 8. Verify rollup metrics recorded
        String summaryRes = mockMvc.perform(get("/api/v1/metrics/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode summary = objectMapper.readTree(summaryRes);
        assertThat(summary.get("totalRequests").asLong()).isGreaterThanOrEqualTo(3L);
    }
}
