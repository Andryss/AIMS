package gov.mib.aims.backend.security;

import gov.mib.aims.backend.config.IntegrationMonitoringProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Аутентификация push-запросов внешней системы мониторинга по заголовку {@code X-Integration-Api-Key}.
 */
@Component
@RequiredArgsConstructor
public class IntegrationApiKeyFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Integration-Api-Key";
    public static final String INGEST_PATH = "/api/v1/integration/monitoring/events";

    private final IntegrationMonitoringProperties integrationMonitoringProperties;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (isIngestRequest(request) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String configuredKey = integrationMonitoringProperties.getApiKey();
            String providedKey = request.getHeader(API_KEY_HEADER);
            if (configuredKey != null
                    && !configuredKey.isBlank()
                    && Objects.equals(configuredKey, providedKey)) {
                SecurityContextHolder.getContext().setAuthentication(new IntegrationAuthentication());
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isIngestRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && INGEST_PATH.equals(request.getRequestURI());
    }
}
