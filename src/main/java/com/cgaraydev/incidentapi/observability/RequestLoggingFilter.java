package com.cgaraydev.incidentapi.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String MDC_KEY = "requestId";
    private static final String RESPONSE_HEADER = "X-Request-ID";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        return !path.equals("/api") && !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String previousRequestId = MDC.get(MDC_KEY);
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        boolean completed = false;

        MDC.put(MDC_KEY, requestId);
        response.setHeader(RESPONSE_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            try {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedAt);

                Object route = request.getAttribute(
                        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

                int status = completed
                        ? response.getStatus()
                        : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

                log.info(
                        "HTTP request completed method={} route={} status={} durationMs={}",
                        request.getMethod(),
                        route != null ? route : "UNMAPPED",
                        status,
                        durationMs);
            } finally {
                if (previousRequestId == null) {
                    MDC.remove(MDC_KEY);
                } else {
                    MDC.put(MDC_KEY, previousRequestId);
                }
            }
        }
    }
}