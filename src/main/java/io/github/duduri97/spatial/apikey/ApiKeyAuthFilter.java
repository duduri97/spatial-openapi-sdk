package io.github.duduri97.spatial.apikey;

import io.github.duduri97.spatial.metrics.ApiUsageMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final ApiUsageMetricsService metricsService;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService, ApiUsageMetricsService metricsService) {
        this.apiKeyService = apiKeyService;
        this.metricsService = metricsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/openapi/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String keyValue = request.getHeader("X-API-Key");
        if (keyValue == null || keyValue.isBlank()) {
            keyValue = request.getParameter("apiKey");
        }

        if (keyValue == null || keyValue.isBlank()) {
            writeJsonError(response, 401, "API_KEY_MISSING", "Provide X-API-Key header or apiKey query parameter.");
            return;
        }

        ApiKey apiKey = apiKeyService.findByKeyValue(keyValue);
        if (apiKey == null) {
            writeJsonError(response, 401, "API_KEY_INVALID", "Unknown API key.");
            return;
        }

        if (apiKey.getStatus() != ApiKey.Status.APPROVED) {
            writeJsonError(response, 403, "API_KEY_NOT_APPROVED", "API key status is " + apiKey.getStatus());
            return;
        }

        long usedToday = metricsService.getTodayRequestCount(keyValue);
        if (usedToday >= apiKey.getDailyQuota()) {
            writeJsonError(response, 429, "DAILY_QUOTA_EXCEEDED", "Daily quota limit reached: " + apiKey.getDailyQuota());
            return;
        }

        long start = System.nanoTime();
        boolean isError = false;
        try {
            filterChain.doFilter(request, response);
            isError = response.getStatus() >= 400;
        } catch (Exception ex) {
            isError = true;
            throw ex;
        } finally {
            long latencyMs = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
            metricsService.record(keyValue, request.getRequestURI(), latencyMs, isError);
        }
    }

    private void writeJsonError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
