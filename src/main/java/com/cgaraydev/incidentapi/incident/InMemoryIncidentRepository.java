package com.cgaraydev.incidentapi.incident;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryIncidentRepository {

    private final Map<UUID, Incident> incidents = new ConcurrentHashMap<>();

    public void save(Incident incident) {
        incidents.put(incident.getId(), incident);
    }

    public Optional<Incident> findById(UUID id) {
        return Optional.ofNullable(incidents.get(id));
    }
}