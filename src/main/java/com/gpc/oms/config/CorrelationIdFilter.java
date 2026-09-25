// AI Provenance: generated from docs/02-observability-and-logging.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Top-precedence HTTP Servlet Filter establishing distributed correlation identifiers and MDC context.
 *
 * <p>Mitigates CWE-778 (Insufficient Logging) and bridges Forensic Audit gaps by ensuring every incoming
 * request across the microservice ecosystem is assigned a deterministic or generated tracing identity.</p>
 *
 * @apiNote Intercepts all incoming HTTP requests before any security or application dispatchers.
 *          If the client provides an {@code X-Correlation-Id} header, its sanitized value is retained;
 *          otherwise, an RFC 4122 random UUID is generated. Both {@code traceId} and {@code correlationId}
 *          keys are populated into SLF4J MDC, and the header is propagated on the HTTP response.
 * @implSpec Extends {@link OncePerRequestFilter} to guarantee single execution per request thread dispatch.
 *           Configured with {@link Ordered#HIGHEST_PRECEDENCE} to precede Spring Security filter chains.
 * @implNote Enforces strict ThreadLocal lifecycle hygiene by removing MDC keys in an unconditional
 *           {@code finally} block, preventing context contamination across worker thread reuse in Tomcat.
 * @author GPC OMS Architecture Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://cwe.mitre.org/data/definitions/778.html">CWE-778</a>
 * @see org.slf4j.MDC
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * Standard HTTP header containing the distributed tracing correlation identifier.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /**
     * SLF4J MDC context key adhering to Elastic Common Schema (ECS) specification.
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * SLF4J MDC context key adhering to Outage Work Order domain business audit logging.
     */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /**
     * Default constructor for Spring container instantiation.
     */
    public CorrelationIdFilter() {
        super();
    }

    /**
     * Executes the filter logic, establishing MDC context and propagating correlation headers.
     *
     * @param request The current HTTP servlet request
     * @param response The HTTP servlet response being constructed
     * @param filterChain The downstream filter chain
     * @throws ServletException if an error occurs during downstream chain processing
     * @throws IOException if an I/O error occurs writing or streaming the response
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.trim();
        }

        try {
            MDC.put(TRACE_ID_MDC_KEY, correlationId);
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
