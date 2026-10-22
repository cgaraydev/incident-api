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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

        private final IncidentService service;

        public IncidentController(IncidentService service) {
                this.service = service;
        }

        @PatchMapping("/{id}/status")
        public ResponseEntity<IncidentResponse> changeStatus(
                        @PathVariable("id") UUID id,
                        @Valid @RequestBody ChangeIncidentStatusRequest request) {
                Incident incident = service.changeStatus(id, request.status());

                return ResponseEntity.ok(IncidentResponse.from(incident));
        }

        @PostMapping
        public ResponseEntity<IncidentResponse> create(
                        @Valid @RequestBody CreateIncidentRequest request) {
                Incident incident = service.create(
                                request.title(),
                                request.description());

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
                        @PathVariable("id") UUID id) {
                Incident incident = service.findById(id)
                                .orElseThrow(() -> new IncidentNotFoundException(id));

                return ResponseEntity.ok(IncidentResponse.from(incident));
        }
}