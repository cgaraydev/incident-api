package com.cgaraydev.incidentapi.incident;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIncidentRepository {

    private final JdbcClient jdbc;

    public JdbcIncidentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Incident incident) {
        jdbc.sql("""
                INSERT INTO incidents (
                    id, title, description, status, created_at, updated_at
                )
                VALUES (
                    :id, :title, :description, :status, :createdAt, :updatedAt
                )
                """)
                .param("id", incident.getId())
                .param("title", incident.getTitle())
                .param("description", incident.getDescription())
                .param("status", incident.getStatus().name())
                .param("createdAt", Timestamp.from(incident.getCreatedAt()))
                .param("updatedAt", Timestamp.from(incident.getUpdatedAt()))
                .update();
    }

    public Optional<Incident> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, title, description, status, created_at, updated_at
                FROM incidents
                WHERE id = :id
                """)
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    private Incident mapRow(ResultSet row, int rowNumber)
            throws SQLException {
        return Incident.restore(
                row.getObject("id", UUID.class),
                row.getString("title"),
                row.getString("description"),
                IncidentStatus.valueOf(row.getString("status")),
                row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant());
    }

    public Optional<Incident> findByIdForUpdate(UUID id) {
        return jdbc.sql("""
                SELECT id, title, description, status, created_at, updated_at
                FROM incidents
                WHERE id = :id
                FOR UPDATE
                """)
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public void updateStatus(Incident incident) {
        int affectedRows = jdbc.sql("""
                UPDATE incidents
                SET status = :status,
                    updated_at = :updatedAt
                WHERE id = :id
                """)
                .param("status", incident.getStatus().name())
                .param("updatedAt", Timestamp.from(incident.getUpdatedAt()))
                .param("id", incident.getId())
                .update();

        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "Expected to update exactly one incident");
        }
    }

    public List<Incident> findPage(
            int limit,
            long offset,
            IncidentStatus status) {

        String sql = """
                SELECT id, title, description, status, created_at, updated_at
                FROM incidents
                """;

        if (status != null) {
            sql += " WHERE status = :status";
        }

        sql += """
                 ORDER BY created_at DESC, id DESC
                 LIMIT :limit OFFSET :offset
                """;

        var query = jdbc.sql(sql)
                .param("limit", limit)
                .param("offset", offset);

        if (status != null) {
            query = query.param("status", status.name());
        }

        return query.query(this::mapRow).list();
    }

}