package com.cgaraydev.incidentapi.incident;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService service;

    public IncidentController(IncidentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(
            @Valid @RequestBody CreateIncidentRequest request
    ) {
        Incident incident = service.create(
                request.title(),
                request.description()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(incident.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(IncidentResponse.from(incident));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> findById(
            @PathVariable("id") UUID id
    ) {
        return service.findById(id)
                .map(IncidentResponse::from)
                .map(response -> ResponseEntity.ok(response))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}