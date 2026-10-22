package com.cgaraydev.incidentapi.analysis;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class IncidentAnalysisParserTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final IncidentAnalysisParser parser =
            new IncidentAnalysisParser(jsonMapper);

    @Test
    void acceptsAValidAnalysis() {
        IncidentAnalysis analysis = parser.parse("""
                {
                  "suggestedCategory": "HARDWARE",
                  "suggestedSummary": "El equipo no enciende."
                }
                """);

        assertEquals(
                IncidentCategory.HARDWARE,
                analysis.suggestedCategory()
        );
        assertEquals(
                "El equipo no enciende.",
                analysis.suggestedSummary()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-json",
            "null",
            "{}",
            """
            {"suggestedCategory":"HARDWARE","suggestedSummary":""}
            """,
            """
            {"suggestedCategory":"UNKNOWN","suggestedSummary":"Resumen"}
            """,
            """
            {"suggestedCategory":0,"suggestedSummary":"Resumen"}
            """,
            """
            {"suggestedCategory":"HARDWARE","suggestedSummary":"Resumen","extra":true}
            """,
            """
            {"suggestedCategory":"HARDWARE","suggestedSummary":"Resumen"} {}
            """
    })
    void rejectsInvalidResponses(String response) {
        assertThrows(RuntimeException.class, () -> parser.parse(response));
    }

    @Test
    void rejectsASummaryLongerThanTheAllowedLimit() {
        String response = jsonMapper.writeValueAsString(Map.of(
                "suggestedCategory", "HARDWARE",
                "suggestedSummary", "a".repeat(501)
        ));

        assertThrows(RuntimeException.class, () -> parser.parse(response));
    }
}