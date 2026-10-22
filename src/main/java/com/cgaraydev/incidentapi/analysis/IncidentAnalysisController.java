package com.cgaraydev.incidentapi.analysis;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentAnalysisController {

    private final IncidentAnalysisService service;

    public IncidentAnalysisController(IncidentAnalysisService service) {
        this.service = service;
    }

    @PostMapping("/{id}/analysis")
    public IncidentAnalysis analyze(@PathVariable("id") UUID id) {
        return service.analyze(id);
    }
}