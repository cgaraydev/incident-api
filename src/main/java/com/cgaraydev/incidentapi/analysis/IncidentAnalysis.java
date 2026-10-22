package com.cgaraydev.incidentapi.analysis;

import java.util.Objects;

public record IncidentAnalysis(
        IncidentCategory suggestedCategory,
        String suggestedSummary
) {

    public IncidentAnalysis {
        Objects.requireNonNull(
                suggestedCategory,
                "Suggested category must not be null"
        );

        if (suggestedSummary == null || suggestedSummary.isBlank()) {
            throw new IllegalArgumentException(
                    "Suggested summary must not be blank"
            );
        }

        if (suggestedSummary.length() > 500) {
            throw new IllegalArgumentException(
                    "Suggested summary must not exceed 500 characters"
            );
        }
    }
}