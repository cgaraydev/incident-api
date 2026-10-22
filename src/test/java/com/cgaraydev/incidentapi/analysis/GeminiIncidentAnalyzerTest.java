package com.cgaraydev.incidentapi.analysis;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class GeminiIncidentAnalyzerTest {

        private final JsonMapper jsonMapper = JsonMapper.builder().build();

        private final AtomicInteger responseStatus = new AtomicInteger(200);
        private final AtomicReference<String> responseBody = new AtomicReference<>("");
        private final AtomicBoolean holdResponse = new AtomicBoolean(false);
        private final CountDownLatch releaseResponse = new CountDownLatch(1);

        private HttpServer server;
        private Client client;
        private GeminiIncidentAnalyzer analyzer;

        @BeforeEach
        void setUp() throws Exception {
                server = HttpServer.create(
                                new InetSocketAddress("127.0.0.1", 0),
                                0);

                server.createContext("/", exchange -> {
                        try {
                                exchange.getRequestBody().readAllBytes();

                                if (holdResponse.get()) {
                                        try {
                                                releaseResponse.await(5, TimeUnit.SECONDS);
                                        } catch (InterruptedException exception) {
                                                Thread.currentThread().interrupt();
                                                return;
                                        }
                                }

                                byte[] body = responseBody.get()
                                                .getBytes(StandardCharsets.UTF_8);

                                exchange.getResponseHeaders()
                                                .set("Content-Type", "application/json");

                                exchange.sendResponseHeaders(responseStatus.get(), body.length);
                                exchange.getResponseBody().write(body);
                        } finally {
                                exchange.close();
                        }
                });

                server.start();

                String baseUrl = "http://127.0.0.1:"
                                + server.getAddress().getPort();

                client = Client.builder()
                                .vertexAI(false)
                                .apiKey("local-test-key")
                                .httpOptions(HttpOptions.builder()
                                                .baseUrl(baseUrl)
                                                .timeout(5000)
                                                .retryOptions(HttpRetryOptions.builder()
                                                                .attempts(1)
                                                                .build())
                                                .build())
                                .build();

                AnalysisProperties properties = new AnalysisProperties(
                                true,
                                "test-model",
                                Duration.ofSeconds(5),
                                512);

                analyzer = new GeminiIncidentAnalyzer(
                                client,
                                properties,
                                jsonMapper,
                                new IncidentAnalysisParser(jsonMapper));
        }

        @AfterEach
        void tearDown() {
                if (client != null) {
                        client.close();
                }

                if (server != null) {
                        server.stop(0);
                }
        }

        @Test
        void returnsTheStructuredAnalysis() {
                respondWithGeneratedText("""
                                {
                                  "suggestedCategory": "HARDWARE",
                                  "suggestedSummary": "El equipo no enciende."
                                }
                                """, "STOP");

                IncidentAnalysis result = analyzer.analyze(
                                "Equipo no enciende",
                                "La pantalla permanece apagada.");

                assertEquals(IncidentCategory.HARDWARE, result.suggestedCategory());
                assertEquals("El equipo no enciende.", result.suggestedSummary());
        }

        @Test
        void rejectsInvalidGeneratedJson() {
                respondWithGeneratedText("not-json", "STOP");

                assertThrows(AnalysisUnavailableException.class,
                                () -> analyzer.analyze("Título", "Descripción"));
        }

        @Test
        void rejectsAnIncompleteGeneration() {
                respondWithGeneratedText("{}", "MAX_TOKENS");

                assertThrows(AnalysisUnavailableException.class,
                                () -> analyzer.analyze("Título", "Descripción"));
        }

        @Test
        void translatesProviderFailureToAnalysisUnavailable() {
                responseStatus.set(503);
                responseBody.set("""
                                {
                                  "error": {
                                    "code": 503,
                                    "message": "Temporarily unavailable",
                                    "status": "UNAVAILABLE"
                                  }
                                }
                                """);

                AnalysisUnavailableException error = assertThrows(
                                AnalysisUnavailableException.class,
                                () -> analyzer.analyze("Título", "Descripción"));

                assertEquals(
                                "Incident analysis could not be completed",
                                error.getMessage());
                assertNotNull(error.getCause());
        }

        @Test
        void translatesTimeoutToAnalysisUnavailable() {
                respondWithGeneratedText("""
                                {
                                  "suggestedCategory": "HARDWARE",
                                  "suggestedSummary": "El equipo no enciende."
                                }
                                """, "STOP");

                holdResponse.set(true);

                String baseUrl = "http://127.0.0.1:"
                                + server.getAddress().getPort();

                try (Client timeoutClient = Client.builder()
                                .vertexAI(false)
                                .apiKey("local-test-key")
                                .httpOptions(HttpOptions.builder()
                                                .baseUrl(baseUrl)
                                                .timeout(500)
                                                .retryOptions(HttpRetryOptions.builder()
                                                                .attempts(1)
                                                                .build())
                                                .build())
                                .build()) {

                        AnalysisProperties properties = new AnalysisProperties(
                                        true,
                                        "test-model",
                                        Duration.ofMillis(500),
                                        512);

                        GeminiIncidentAnalyzer timeoutAnalyzer = new GeminiIncidentAnalyzer(
                                        timeoutClient,
                                        properties,
                                        jsonMapper,
                                        new IncidentAnalysisParser(jsonMapper));

                        AnalysisUnavailableException error = assertThrows(
                                        AnalysisUnavailableException.class,
                                        () -> timeoutAnalyzer.analyze(
                                                        "Equipo no enciende",
                                                        "La pantalla permanece apagada."));

                        Throwable cause = error;
                        while (cause != null
                                        && !(cause instanceof InterruptedIOException)) {
                                cause = cause.getCause();
                        }

                        assertNotNull(
                                        cause,
                                        "The failure must originate from an HTTP timeout");
                } finally {
                        releaseResponse.countDown();
                }
        }

        private void respondWithGeneratedText(String text, String finishReason) {
                responseBody.set(jsonMapper.writeValueAsString(Map.of(
                                "candidates", List.of(Map.of(
                                                "content", Map.of(
                                                                "role", "model",
                                                                "parts", List.of(Map.of("text", text))),
                                                "finishReason", finishReason)))));
        }
}