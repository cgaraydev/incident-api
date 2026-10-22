package com.cgaraydev.incidentapi.incident;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private final JdbcIncidentRepository repository;

    public IncidentService(JdbcIncidentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Incident create(String title, String description) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Incident incident = Incident.create(title, description, now);

        repository.insert(incident);

        return incident;
    }

    @Transactional
    public Incident changeStatus(UUID id, IncidentStatus target) {
        Incident incident = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        IncidentStatus previousStatus = incident.getStatus();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        incident.changeStatus(target, now);

        if (incident.getStatus() != previousStatus) {
            repository.updateStatus(incident);
        }

        return incident;
    }

    @Transactional(readOnly = true)
    public Optional<Incident> findById(UUID id) {
        return repository.findById(id);
    }
}