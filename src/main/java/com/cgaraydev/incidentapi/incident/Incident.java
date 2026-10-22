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
        this(
                UUID.randomUUID(),
                title,
                description,
                IncidentStatus.OPEN,
                now,
                now);
    }

    private Incident(
            UUID id,
            String title,
            String description,
            IncidentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(
                createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(
                updatedAt, "updatedAt must not be null");

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "updatedAt must not precede createdAt");
        }
    }

    public static Incident create(String title, String description, Instant now) {
        return new Incident(title, description, now);
    }

    static Incident restore(
            UUID id,
            String title,
            String description,
            IncidentStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new Incident(
                id,
                title,
                description,
                status,
                createdAt,
                updatedAt);
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