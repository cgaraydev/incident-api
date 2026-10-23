package com.cgaraydev.incidentapi.incident;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

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

    @Transactional(readOnly = true)
    public IncidentPageResponse findPage(
            int page,
            int size,
            IncidentStatus status) {

        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100");
        }

        long offset = (long) page * size;

        List<Incident> incidents = repository.findPage(
                size + 1,
                offset,
                status);

        boolean hasNext = incidents.size() > size;

        List<IncidentResponse> content = incidents.stream()
                .limit(size)
                .map(IncidentResponse::from)
                .toList();

        return new IncidentPageResponse(content, page, size, hasNext);
    }
}