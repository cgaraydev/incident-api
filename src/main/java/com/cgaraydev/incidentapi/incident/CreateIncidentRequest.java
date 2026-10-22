package com.cgaraydev.incidentapi.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(

        @NotBlank(message = "Title must not be blank")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        @NotBlank(message = "Description must not be blank")
        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description

) {
}