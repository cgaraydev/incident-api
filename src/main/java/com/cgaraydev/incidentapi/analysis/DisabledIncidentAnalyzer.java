package com.cgaraydev.incidentapi.analysis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.analysis.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledIncidentAnalyzer implements IncidentAnalyzer {

    @Override
    public IncidentAnalysis analyze(String title, String description) {
        throw new AnalysisUnavailableException(
                "Incident analysis is disabled"
        );
    }
}