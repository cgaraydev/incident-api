package com.cgaraydev.incidentapi.incident;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentStatusTest {

    @Test
    void allowsTransitionsOfTheIncidentLifecycle() {
        assertTrue(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.IN_PROGRESS));
        assertTrue(IncidentStatus.IN_PROGRESS.canTransitionTo(IncidentStatus.RESOLVED));
        assertTrue(IncidentStatus.RESOLVED.canTransitionTo(IncidentStatus.IN_PROGRESS));
    }

    @Test
    void rejectsTransitionsOutsideTheIncidentLifecycle() {
        assertFalse(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.RESOLVED));
        assertFalse(IncidentStatus.IN_PROGRESS.canTransitionTo(IncidentStatus.OPEN));
        assertFalse(IncidentStatus.RESOLVED.canTransitionTo(IncidentStatus.OPEN));
    }

    @Test
    void allowsKeepingTheCurrentStatus() {
        for (IncidentStatus status : IncidentStatus.values()) {
            assertTrue(status.canTransitionTo(status));
        }
    }

    @Test
    void rejectsNullAsTargetStatus() {
        for (IncidentStatus status : IncidentStatus.values()) {
            assertFalse(status.canTransitionTo(null));
        }
    }
}