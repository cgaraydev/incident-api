package com.cgaraydev.incidentapi.analysis;

import java.util.Objects;

import org.springframework.stereotype.Component;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class IncidentAnalysisParser {

    private final JsonMapper jsonMapper;

    public IncidentAnalysisParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public IncidentAnalysis parse(String text) {
        if (text == null || text.isBlank() || text.length() > 4096) {
            throw new IllegalArgumentException(
                    "Analysis response is empty or exceeds the size limit"
            );
        }

        IncidentAnalysis analysis = jsonMapper
                .readerFor(IncidentAnalysis.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readValue(text);

        return Objects.requireNonNull(
                analysis,
                "Analysis response must not be null"
        );
    }
}