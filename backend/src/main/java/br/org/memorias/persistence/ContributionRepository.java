package br.org.memorias.persistence;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import br.org.memorias.domain.ArchiveModels.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ContributionRepository {
    private final JdbcClient db;
    private final AttachmentRepository attachments;

    ContributionRepository(JdbcClient db, AttachmentRepository attachments) {
        this.db = db;
        this.attachments = attachments;
    }

    public List<Contribution> contributions(int limit) {
        var contributions = db.sql("""
                select a.*, c.title as collection from contributions a
                join collections c on c.slug=a.collection_slug
                order by a.created_at desc, a.id desc limit :limit
                """).param("limit", limit).query((row, index) -> new Contribution(
                row.getLong("id"), row.getString("name"), row.getString("email"), row.getString("title"),
                row.getString("material_type"), row.getString("description"), row.getString("place"),
                row.getString("period"), row.getString("source"), row.getString("collection"),
                row.getString("status"), row.getObject("created_at", OffsetDateTime.class),
                row.getObject("related_memory", Long.class), List.of())).list();
        return contributions.stream().map(item -> item.withFiles(attachments.files(item.id(), true))).toList();
    }

    public long count(String status) {
        return db.sql("select count(*) from contributions where status=:status")
                .param("status", status).query(Long.class).single();
    }
}
