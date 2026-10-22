package com.cgaraydev.incidentapi.incident;

import java.time.Instant;
import java.util.UUID;

import com.cgaraydev.incidentapi.PostgresTestConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class JdbcIncidentRepositoryTest {

    @Autowired
    private JdbcIncidentRepository repository;

    @Test
    void insertsAndRetrievesAnIncident() {
        Instant createdAt = Instant.parse("2026-10-22T20:00:00Z");
        Instant updatedAt = createdAt.plusSeconds(60);

        Incident original = Incident.create(
                "Equipo no enciende",
                "La pantalla permanece apagada.",
                createdAt
        );
        original.changeStatus(IncidentStatus.IN_PROGRESS, updatedAt);

        repository.insert(original);

        Incident restored = repository.findById(original.getId())
                .orElseThrow();

        assertNotSame(original, restored);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getTitle(), restored.getTitle());
        assertEquals(original.getDescription(), restored.getDescription());
        assertEquals(IncidentStatus.IN_PROGRESS, restored.getStatus());
        assertEquals(createdAt, restored.getCreatedAt());
        assertEquals(updatedAt, restored.getUpdatedAt());
    }

    @Test
    void returnsEmptyForAnUnknownIdentifier() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }
}