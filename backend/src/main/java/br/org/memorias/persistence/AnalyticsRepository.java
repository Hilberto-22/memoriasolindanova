package br.org.memorias.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsRepository {
    private final JdbcClient db;

    public AnalyticsRepository(JdbcClient db) {
        this.db = db;
    }

    public List<Count> types() {
        return grouped("memories", "material_type");
    }

    public List<Count> statuses() {
        return grouped("contributions", "status");
    }

    private List<Count> grouped(String table, String column) {
        return db.sql("select " + column + " as label, count(*) as total from " + table
                + " group by " + column + " order by total desc, label")
                .query((rs, row) -> new Count(rs.getString("label"), rs.getLong("total"))).list();
    }

    public long collaborators() {
        return db.sql("select count(distinct lower(email)) from contributions where email <> ''").query(Long.class).single();
    }

    public List<LocalDate> submissionDates(OffsetDateTime from) {
        return db.sql("select created_at from contributions where created_at>=:from")
                .param("from", from).query((rs, row) -> rs.getObject("created_at", OffsetDateTime.class)
                        .atZoneSameInstant(from.toZonedDateTime().getZone()).toLocalDate())
                .list();
    }

    public record Count(String label, long count) {
    }
}
