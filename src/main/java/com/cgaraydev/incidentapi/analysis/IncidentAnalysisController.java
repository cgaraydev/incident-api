package com.cgaraydev.incidentapi.analysis;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Análisis", description = "Sugerencias generadas mediante Gemini")
@RestController
@RequestMapping("/api/incidents")
public class IncidentAnalysisController {

    private final IncidentAnalysisService service;

    public IncidentAnalysisController(IncidentAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "Analizar una incidencia con Gemini", description = "Envía el título y la descripción a Google para sugerir "
            + "una categoría y un resumen. Utilizar únicamente datos ficticios. "
            + "Las sugerencias pueden contener errores. "
            + "El resultado no se guarda ni modifica la incidencia.")
    @ApiResponse(responseCode = "200", description = "Análisis completado")
    @ApiResponse(responseCode = "400", description = "Identificador con formato inválido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Incidencia inexistente", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Análisis desactivado o temporalmente no disponible", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/analysis")
    public IncidentAnalysis analyze(@PathVariable("id") UUID id) {
        return service.analyze(id);
    }
}