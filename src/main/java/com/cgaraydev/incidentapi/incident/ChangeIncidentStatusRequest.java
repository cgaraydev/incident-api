package com.cgaraydev.incidentapi.incident;

import jakarta.validation.constraints.NotNull;

public record ChangeIncidentStatusRequest(
        @NotNull(message = "Status must not be null")
        IncidentStatus status
) {
}