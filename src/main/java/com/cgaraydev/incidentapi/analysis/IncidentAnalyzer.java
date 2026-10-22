package com.cgaraydev.incidentapi.analysis;

public interface IncidentAnalyzer {

    IncidentAnalysis analyze(String title, String description);
}