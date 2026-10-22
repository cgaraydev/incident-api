package com.cgaraydev.incidentapi.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/api/status")
    public StatusResponse getStatus() {
        return new StatusResponse("incident-api", "ok");
    }

    public record StatusResponse(String application, String status) {
    }
}