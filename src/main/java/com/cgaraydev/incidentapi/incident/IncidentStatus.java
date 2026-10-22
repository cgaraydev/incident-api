package com.cgaraydev.incidentapi.incident;

public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED;

    public boolean canTransitionTo(IncidentStatus target) {
        if (target == null) {
            return false;
        }

        if (this == target) {
            return true;
        }

        return switch (this) {
            case OPEN -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == RESOLVED;
            case RESOLVED -> target == IN_PROGRESS;
        };
    }
}