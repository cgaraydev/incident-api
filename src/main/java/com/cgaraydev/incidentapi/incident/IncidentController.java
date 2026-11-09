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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ProblemDetail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Incidencias", description = "Registro, consulta y cambios de estado de incidencias técnicas")
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

        private final IncidentService service;

        public IncidentController(IncidentService service) {
                this.service = service;
        }

        @Operation(summary = "Cambiar el estado de una incidencia", description = "Permite OPEN → IN_PROGRESS, IN_PROGRESS → RESOLVED "
                        + "y RESOLVED → IN_PROGRESS. "
                        + "Solicitar el estado actual conserva la fecha de actualización.")
        @ApiResponse(responseCode = "200", description = "Estado solicitado aplicado")
        @ApiResponse(responseCode = "400", description = "Identificador, JSON o estado solicitado inválido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Incidencia inexistente", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "409", description = "La transición desde el estado actual no está permitida", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
        @PatchMapping("/{id}/status")
        public ResponseEntity<IncidentResponse> changeStatus(
                        @PathVariable("id") UUID id,
                        @Valid @RequestBody ChangeIncidentStatusRequest request) {
                Incident incident = service.changeStatus(id, request.status());

                return ResponseEntity.ok(IncidentResponse.from(incident));
        }

        @Operation(summary = "Registrar una incidencia", description = "Crea una incidencia con estado OPEN. "
                        + "La cabecera Location contiene la dirección del recurso creado.")
        @ApiResponse(responseCode = "201", description = "Incidencia creada")
        @ApiResponse(responseCode = "400", description = "JSON inválido o campos que no cumplen las validaciones", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
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

        @Operation(summary = "Consultar una incidencia por su identificador")
        @ApiResponse(responseCode = "200", description = "Incidencia encontrada")
        @ApiResponse(responseCode = "400", description = "El identificador no tiene un formato UUID válido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "No existe una incidencia con ese identificador", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
        @GetMapping("/{id}")
        public ResponseEntity<IncidentResponse> findById(
                        @PathVariable("id") UUID id) {
                Incident incident = service.findById(id)
                                .orElseThrow(() -> new IncidentNotFoundException(id));

                return ResponseEntity.ok(IncidentResponse.from(incident));
        }

        @Operation(summary = "Listar incidencias", description = "Devuelve una página ordenada desde la incidencia más reciente. "
                        + "La numeración empieza en 0 y el tamaño permitido es de 1 a 100. "
                        + "El filtro status es opcional. "
                        + "hasNext indica si existe una página siguiente.")
        @ApiResponse(responseCode = "200", description = "Página de incidencias")
        @ApiResponse(responseCode = "400", description = "Número de página, tamaño o estado inválido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
        @GetMapping
        public ResponseEntity<IncidentPageResponse> findPage(
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "20") int size,
                        @RequestParam(name = "status", required = false) IncidentStatus status) {

                if (page < 0 || size < 1 || size > 100) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Page must be non-negative and size must be between 1 and 100");
                }

                return ResponseEntity.ok(service.findPage(page, size, status));
        }
}