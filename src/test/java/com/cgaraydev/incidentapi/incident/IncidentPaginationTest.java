package com.cgaraydev.incidentapi.incident;

import java.time.Instant;
import java.util.UUID;

import com.cgaraydev.incidentapi.PostgresTestConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class IncidentPaginationTest {

        private static final UUID OLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

        private static final UUID NEW_LOW_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

        private static final UUID NEW_HIGH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcClient jdbc;

        @Autowired
        private JdbcIncidentRepository repository;

        @BeforeEach
        void prepareIncidents() {
                jdbc.sql("DELETE FROM incidents").update();

                insertIncident(OLD_ID, "2026-01-01T10:00:00Z");
                insertIncident(NEW_LOW_ID, "2026-01-02T10:00:00Z");
                insertIncident(NEW_HIGH_ID, "2026-01-02T10:00:00Z");
        }

        @Test
        void ordersByCreationDateAndUsesIdToBreakTies() throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("page", "0")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(NEW_HIGH_ID.toString()))
                                .andExpect(jsonPath("$.content[1].id")
                                                .value(NEW_LOW_ID.toString()))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(2))
                                .andExpect(jsonPath("$.hasNext").value(true));
        }

        @Test
        void returnsTheLastPageWithoutRepeatingPreviousItems() throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("page", "1")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(OLD_ID.toString()))
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void returnsAnEmptyPageBeyondTheAvailableResults() throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("page", "2")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.page").value(2))
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void usesDefaultPaginationWhenParametersAreAbsent() throws Exception {
                mockMvc.perform(get("/api/incidents"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(3))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(20))
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void returnsAnEmptyPageWhenThereAreNoIncidents() throws Exception {
                jdbc.sql("DELETE FROM incidents").update();

                mockMvc.perform(get("/api/incidents"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void reportsNoNextPageWhenThePageIsExactlyFull() throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("size", "3"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(3))
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @ParameterizedTest
        @CsvSource({
                        "-1, 20",
                        "0, 0",
                        "0, -1",
                        "0, 101",
                        "abc, 20",
                        "0, abc"
        })
        void rejectsInvalidPagination(String page, String size) throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("page", page)
                                .param("size", size))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void supportsLargePageNumbersWithoutIntegerOverflow() throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("page", "2147483647")
                                .param("size", "100"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @ParameterizedTest
        @EnumSource(IncidentStatus.class)
        void filtersByEachSupportedStatus(IncidentStatus expectedStatus)
                        throws Exception {

                prepareMixedStatuses();

                UUID expectedId = switch (expectedStatus) {
                        case OPEN -> NEW_HIGH_ID;
                        case IN_PROGRESS -> NEW_LOW_ID;
                        case RESOLVED -> OLD_ID;
                };

                mockMvc.perform(get("/api/incidents")
                                .param("status", expectedStatus.name()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(expectedId.toString()))
                                .andExpect(jsonPath("$.content[0].status")
                                                .value(expectedStatus.name()))
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void paginatesOnlyMatchingIncidents() throws Exception {
                prepareMixedStatuses();

                UUID newestOpenId = UUID.fromString("00000000-0000-0000-0000-000000000004");

                insertIncident(newestOpenId, "2026-01-03T10:00:00Z");

                mockMvc.perform(get("/api/incidents")
                                .param("status", "OPEN")
                                .param("page", "0")
                                .param("size", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(newestOpenId.toString()))
                                .andExpect(jsonPath("$.hasNext").value(true));

                mockMvc.perform(get("/api/incidents")
                                .param("status", "OPEN")
                                .param("page", "1")
                                .param("size", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id")
                                                .value(NEW_HIGH_ID.toString()))
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void returnsAnEmptyPageWhenNoIncidentsMatchTheStatus() throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("status", "RESOLVED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        void rejectsAnUnknownStatusFilter() throws Exception {
                mockMvc.perform(get("/api/incidents")
                                .param("status", "INVALID"))
                                .andExpect(status().isBadRequest());
        }

        private void prepareMixedStatuses() {
                jdbc.sql("""
                                UPDATE incidents
                                SET status = :status
                                WHERE id = :id
                                """)
                                .param("status", IncidentStatus.RESOLVED.name())
                                .param("id", OLD_ID)
                                .update();

                jdbc.sql("""
                                UPDATE incidents
                                SET status = :status
                                WHERE id = :id
                                """)
                                .param("status", IncidentStatus.IN_PROGRESS.name())
                                .param("id", NEW_LOW_ID)
                                .update();
        }

        private void insertIncident(UUID id, String timestamp) {
                Instant createdAt = Instant.parse(timestamp);

                Incident incident = Incident.restore(
                                id,
                                "Incidencia de prueba",
                                "Descripción de prueba",
                                IncidentStatus.OPEN,
                                createdAt,
                                createdAt);

                repository.insert(incident);
        }
}