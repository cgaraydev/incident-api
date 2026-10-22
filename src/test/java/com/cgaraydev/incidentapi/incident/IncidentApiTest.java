package com.cgaraydev.incidentapi.incident;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentApiTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void createsAnIncidentAndRetrievesItFromItsLocation() throws Exception {
                MvcResult created = mockMvc.perform(post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "title": "Equipo no enciende",
                                                  "description": "La pantalla permanece apagada."
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.title").value("Equipo no enciende"))
                                .andExpect(jsonPath("$.description")
                                                .value("La pantalla permanece apagada."))
                                .andExpect(jsonPath("$.status").value("OPEN"))
                                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                                .andReturn();

                String location = created.getResponse().getHeader("Location");
                assertNotNull(location);

                String createdJson = created.getResponse().getContentAsString();

                mockMvc.perform(get(URI.create(location)))
                                .andExpect(status().isOk())
                                .andExpect(content().json(createdJson));
        }

        @Test
        void rejectsBlankTitle() throws Exception {
                mockMvc.perform(post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "title": "   ",
                                                  "description": "La pantalla permanece apagada."
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Validation failed"))
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail")
                                                .value("One or more fields are invalid."))
                                .andExpect(jsonPath("$.instance").value("/api/incidents"))
                                .andExpect(jsonPath("$.errors.length()").value(1))
                                .andExpect(jsonPath("$.errors[0].field").value("title"))
                                .andExpect(jsonPath("$.errors[0].message")
                                                .value("Title must not be blank"));
        }

        @Test
        void returnsNotFoundForAnUnknownIncident() throws Exception {
                UUID id = UUID.randomUUID();

                mockMvc.perform(get("/api/incidents/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Incident not found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Incident not found: " + id))
                                .andExpect(jsonPath("$.instance")
                                                .value("/api/incidents/" + id));
        }

        @Test
        void rejectsMalformedIdentifier() throws Exception {
                mockMvc.perform(get("/api/incidents/no-es-un-uuid"))
                                .andExpect(status().isBadRequest());
        }
}