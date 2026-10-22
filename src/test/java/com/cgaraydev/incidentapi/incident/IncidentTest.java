package com.cgaraydev.incidentapi.incident;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentTest {

    private static final Instant NOW = Instant.parse("2026-10-22T16:00:00Z");

    @Test
    void createsAnOpenIncidentWithIdentityAndCreationTime() {
        Incident incident = Incident.create(
                "Equipo no inicia",
                "La pantalla permanece apagada al presionar encendido.",
                NOW);

        assertNotNull(incident.getId());
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(NOW, incident.getCreatedAt());
        assertEquals(NOW, incident.getUpdatedAt());
    }

    @Test
    void preservesTheOriginalText() {
        Incident incident = Incident.create(
                "  Equipo no inicia  ",
                "Descripción original.\nSegunda línea.",
                NOW);

        assertEquals("  Equipo no inicia  ", incident.getTitle());
        assertEquals(
                "Descripción original.\nSegunda línea.",
                incident.getDescription());
    }

    @Test
    void rejectsMissingOrBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> Incident.create(null, "Descripción", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Incident.create(" \t\n", "Descripción", NOW));
    }

    @Test
    void rejectsMissingOrBlankDescription() {
        assertThrows(IllegalArgumentException.class,
                () -> Incident.create("Título", null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Incident.create("Título", " \t\n", NOW));
    }

    @Test
    void rejectsMissingCreationTime() {
        assertThrows(NullPointerException.class,
                () -> Incident.create("Título", "Descripción", null));
    }

    @Test
    void changesStatusAndUpdatesModificationTime() {
        Incident incident = Incident.create("Título", "Descripción", NOW);
        Instant later = NOW.plusSeconds(60);

        incident.changeStatus(IncidentStatus.IN_PROGRESS, later);

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals(later, incident.getUpdatedAt());
        assertEquals(NOW, incident.getCreatedAt());
    }

    @Test
    void resolvesAndReopensAnIncident() {
        Incident incident = Incident.create("Título", "Descripción", NOW);

        incident.changeStatus(IncidentStatus.IN_PROGRESS, NOW.plusSeconds(10));
        incident.changeStatus(IncidentStatus.RESOLVED, NOW.plusSeconds(20));

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals(NOW.plusSeconds(20), incident.getUpdatedAt());

        incident.changeStatus(IncidentStatus.IN_PROGRESS, NOW.plusSeconds(30));

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals(NOW.plusSeconds(30), incident.getUpdatedAt());
    }

    @Test
    void rejectsInvalidTransitionWithoutChangingTheIncident() {
        Incident incident = Incident.create("Título", "Descripción", NOW);

        assertThrows(InvalidIncidentStatusTransitionException.class,
                () -> incident.changeStatus(
                        IncidentStatus.RESOLVED, NOW.plusSeconds(60)));

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(NOW, incident.getUpdatedAt());
    }

    @Test
    void keepingCurrentStatusDoesNotUpdateModificationTime() {
        Incident incident = Incident.create("Título", "Descripción", NOW);

        incident.changeStatus(IncidentStatus.OPEN, NOW.plusSeconds(60));

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(NOW, incident.getUpdatedAt());
    }

    @Test
    void rejectsNullStatusWithoutChangingTheIncident() {
        Incident incident = Incident.create("Título", "Descripción", NOW);

        assertThrows(IllegalArgumentException.class,
                () -> incident.changeStatus(null, NOW.plusSeconds(60)));

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(NOW, incident.getUpdatedAt());
    }

    @Test
    void rejectsMissingTimeForAnEffectiveTransition() {
        Incident incident = Incident.create("Título", "Descripción", NOW);

        assertThrows(NullPointerException.class,
                () -> incident.changeStatus(IncidentStatus.IN_PROGRESS, null));

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(NOW, incident.getUpdatedAt());
    }

    @Test
    void rejectsModificationTimeEarlierThanTheLastUpdate() {
        Incident incident = Incident.create("Título", "Descripción", NOW);
        Instant lastUpdate = NOW.plusSeconds(60);
        incident.changeStatus(IncidentStatus.IN_PROGRESS, lastUpdate);

        assertThrows(IllegalArgumentException.class,
                () -> incident.changeStatus(
                        IncidentStatus.RESOLVED, NOW.plusSeconds(30)));

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals(lastUpdate, incident.getUpdatedAt());
    }
}