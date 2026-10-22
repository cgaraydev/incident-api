package com.cgaraydev.incidentapi.incident;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final InMemoryIncidentRepository repository;

    public IncidentService(InMemoryIncidentRepository repository) {
        this.repository = repository;
    }

    public Incident create(String title, String description) {
        Incident incident = Incident.create(
                title,
                description,
                Instant.now());

        repository.save(incident);

        return incident;
    }

    public Incident changeStatus(UUID id, IncidentStatus target) {
        Incident incident = repository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        synchronized (incident) {
            incident.changeStatus(target, Instant.now());
        }

        return incident;
    }

    public Optional<Incident> findById(UUID id) {
        return repository.findById(id);
    }
}