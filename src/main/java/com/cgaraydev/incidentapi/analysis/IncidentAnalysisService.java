package com.cgaraydev.incidentapi.analysis;

import java.util.UUID;

import com.cgaraydev.incidentapi.incident.Incident;
import com.cgaraydev.incidentapi.incident.IncidentNotFoundException;
import com.cgaraydev.incidentapi.incident.IncidentService;

import org.springframework.stereotype.Service;

@Service
public class IncidentAnalysisService {

    private final IncidentService incidentService;
    private final IncidentAnalyzer analyzer;

    public IncidentAnalysisService(
            IncidentService incidentService,
            IncidentAnalyzer analyzer
    ) {
        this.incidentService = incidentService;
        this.analyzer = analyzer;
    }

    public IncidentAnalysis analyze(UUID id) {
        Incident incident = incidentService.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        return analyzer.analyze(
                incident.getTitle(),
                incident.getDescription()
        );
    }
}