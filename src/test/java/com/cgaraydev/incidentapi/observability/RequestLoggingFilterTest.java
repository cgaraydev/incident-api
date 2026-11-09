package com.cgaraydev.incidentapi.observability;

import java.util.UUID;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.MDC;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter =
            new RequestLoggingFilter();

    @Test
    void generatesItsOwnIdentifierAndLogsTheResult(CapturedOutput output)
            throws Exception {

        var request = new MockHttpServletRequest(
                "GET", "/api/incidents");

        request.addHeader("X-Request-ID", "client-supplied-id");
        request.setQueryString("private=value");

        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertEquals(
                    response.getHeader("X-Request-ID"),
                    MDC.get("requestId"));

            req.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/incidents");

            response.setStatus(400);
        });

        String requestId = response.getHeader("X-Request-ID");

        assertNotNull(requestId);
        assertDoesNotThrow(() -> UUID.fromString(requestId));
        assertNotEquals("client-supplied-id", requestId);

        assertTrue(output.getOut().contains(
                "method=GET route=/api/incidents status=400 durationMs="));

        assertFalse(output.getOut().contains("private=value"));
    }

    @Test
    void restoresThePreviousLoggingContext() throws Exception {
        String original = MDC.get("requestId");
        MDC.put("requestId", "previous-context");

        try {
            var request = new MockHttpServletRequest(
                    "GET", "/api/status");

            var response = new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) ->
                    assertNotEquals(
                            "previous-context",
                            MDC.get("requestId")));

            assertEquals("previous-context", MDC.get("requestId"));
        } finally {
            restoreRequestId(original);
        }
    }

    @Test
    void propagatesExceptionsAndCleansTheLoggingContext(
            CapturedOutput output) {

        String original = MDC.get("requestId");
        MDC.remove("requestId");

        try {
            var request = new MockHttpServletRequest(
                    "GET", "/api/incidents");

            var response = new MockHttpServletResponse();
            var failure = new ServletException("Simulated failure");

            ServletException thrown = assertThrows(
                    ServletException.class,
                    () -> filter.doFilter(request, response, (req, res) -> {
                        throw failure;
                    }));

            assertSame(failure, thrown);
            assertNull(MDC.get("requestId"));

            assertTrue(output.getOut().contains(
                    "route=UNMAPPED status=500 durationMs="));
        } finally {
            restoreRequestId(original);
        }
    }

    @Test
    void skipsStaticResources() throws Exception {
        var request = new MockHttpServletRequest(
                "GET", "/js/app.js");

        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertNull(response.getHeader("X-Request-ID"));
    }

    private static void restoreRequestId(String previous) {
        if (previous == null) {
            MDC.remove("requestId");
        } else {
            MDC.put("requestId", previous);
        }
    }
}