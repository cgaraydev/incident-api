package com.cgaraydev.incidentapi.incident;

public class InvalidIncidentStatusTransitionException
        extends IllegalArgumentException {

    public InvalidIncidentStatusTransitionException(
            IncidentStatus current,
            IncidentStatus target
    ) {
        super("Cannot transition from " + current + " to " + target);
    }
}