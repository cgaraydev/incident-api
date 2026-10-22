package com.cgaraydev.incidentapi.incident;

import java.util.List;

public record IncidentPageResponse(
        List<IncidentResponse> content,
        int page,
        int size,
        boolean hasNext) {

    public IncidentPageResponse {
        content = List.copyOf(content);
    }
}