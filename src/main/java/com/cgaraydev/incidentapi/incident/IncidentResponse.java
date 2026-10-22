package com.cgaraydev.incidentapi.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        String description,
        IncidentStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getCreatedAt(),
                incident.getUpdatedAt());
    }
}