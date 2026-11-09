package com.cgaraydev.incidentapi;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsThePublicApiContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Incident API"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.paths['/api/status'].get").exists())
                .andExpect(jsonPath("$.paths['/api/incidents'].get").exists())
                .andExpect(jsonPath("$.paths['/api/incidents'].post").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/incidents/{id}'].get").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/incidents/{id}/status'].patch").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/incidents/{id}/analysis'].post").exists());
    }
}