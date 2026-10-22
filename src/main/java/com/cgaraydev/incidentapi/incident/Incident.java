package com.cgaraydev.incidentapi.incident;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Incident {

    private final UUID id;
    private final String title;
    private final String description;
    private final Instant createdAt;

    private IncidentStatus status;
    private Instant updatedAt;

    private Incident(String title, String description, Instant now) {
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.createdAt = Objects.requireNonNull(now, "now must not be null");
        this.updatedAt = now;
        this.id = UUID.randomUUID();
        this.status = IncidentStatus.OPEN;
    }

    public static Incident create(String title, String description, Instant now) {
        return new Incident(title, description, now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public void changeStatus(IncidentStatus target, Instant now) {
        if (target == null) {
            throw new IllegalArgumentException("Target status must not be null");
        }

        if (!status.canTransitionTo(target)) {
            throw new InvalidIncidentStatusTransitionException(status, target);
        }

        if (status == target) {
            return;
        }

        Objects.requireNonNull(now, "now must not be null");

        if (now.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "Status change time must not precede updatedAt");
        }

        this.status = target;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}