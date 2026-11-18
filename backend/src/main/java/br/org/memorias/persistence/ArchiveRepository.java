package br.org.memorias.persistence;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import br.org.memorias.domain.ArchiveModels.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ArchiveRepository {
    private final JdbcClient db;
    private final AttachmentRepository attachments;

    ArchiveRepository(JdbcClient db, AttachmentRepository attachments) {
        this.db = db;
        this.attachments = attachments;
    }

    static final String SELECT = """
            select m.*, c.title as collection,
              (select min(f.id) from memory_files f where f.contribution_id=m.contribution_id
                and f.content_type like 'image/%') as cover_id
            from memories m join collections c on c.slug=m.collection_slug
            """;
    public Page catalog(String query, String collection, String category, String type, String year, Integer from, Integer to, String place,
                 String sort, int requestedPage, int requestedSize, boolean withImage) {
        var conditions = new ArrayList<String>();
        var parameters = new HashMap<String, Object>();
        if (withImage) {
            conditions.add("""
                    exists (select 1 from memory_files f where f.contribution_id=m.contribution_id
                            and f.content_type like 'image/%')
                    """);
        }
        if (!query.isBlank()) {
            parameters.put("query", pattern(query));
            conditions.add("""
                    (lower(m.title) like :query escape '!' or lower(m.description) like :query escape '!'
                     or lower(m.place) like :query escape '!')
                    """);
        }
        exact(conditions, parameters, "collection_slug", collection);
        exact(conditions, parameters, "category", category);
        if (!type.isBlank()) {
            parameters.put("types", Arrays.asList(type.split(",", -1)));
            conditions.add("m.material_type in (:types)");
        }
        exact(conditions, parameters, "place", place);
        if (from != null) {
            parameters.put("from",from);
            conditions.add("m.historical_year>=:from");
        }
        if (to != null) {
            parameters.put("to",to);
            conditions.add("m.historical_year<=:to");
        }
        if (!year.isBlank()) {
            if (year.matches("[0-9]{1,4}")) {
                parameters.put("year", Integer.parseInt(year));
                conditions.add("m.historical_year=:year");
            } else if (year.matches("(?i)(anos |década de )?[0-9]{3}0s?")) {
                int firstYear = Integer.parseInt(year.replaceAll("[^0-9]", ""));
                parameters.put("startYear", firstYear);
                parameters.put("endYear", firstYear + 9);
                conditions.add("m.historical_year between :startYear and :endYear");
            } else {
                parameters.put("period", pattern(year));
                conditions.add("lower(m.period) like :period escape '!'");
            }
        }
        String where = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
        long total = db.sql("select count(*) from memories m" + where).params(parameters).query(Long.class).single();
        int size = Math.clamp(requestedSize, 1, 48);
        int page = (int) Math.clamp(requestedPage, 0L, Math.max(0L, (total - 1) / size));
        String order = switch (sort) {
            case "oldest" -> "m.historical_year asc nulls last, m.id asc";
            case "title" -> "lower(m.title), m.id asc";
            default -> "m.published_at desc, m.id desc";
        };
        var records = db.sql(SELECT + where + " order by " + order + " limit :size offset :offset")
                .params(parameters).param("size", size).param("offset", (long) page * size)
                .query(this::mapMemory).list();
        return new Page(records, total, page, size);
    }

    public Facets facets() {
        return new Facets(distinct("material_type"), distinct("category"), distinct("place"));
    }

    private List<String> distinct(String column) {
        return db.sql("select distinct " + column + " from memories where " + column + " <> '' order by " + column)
                .query(String.class).list();
    }

    public Memory detail(long id) {
        return db.sql(SELECT + " where m.id=:id").param("id", id).query(this::mapMemory).optional()
                .map(memory -> memory.withFiles(attachments.files(memory.contributionId(), false))).orElse(null);
    }

    public boolean published(long id) {
        return db.sql("select count(*) from memories where id=:id").param("id", id)
                .query(Long.class).single() > 0;
    }

    public List<Memory> map() {
        return db.sql(SELECT + " where m.latitude is not null order by m.id")
                .query(this::mapMemory).list();
    }

    public List<Memory> timeline() {
        return db.sql(SELECT + " where m.historical_year is not null order by m.historical_year, m.id")
                .query(this::mapMemory).list();
    }

    private Memory mapMemory(ResultSet row, int index) throws SQLException {
        return new Memory(row.getLong("id"), row.getLong("contribution_id"), row.getString("title"),
                row.getString("material_type"), row.getString("category"), row.getString("collection"),
                row.getString("collection_slug"), row.getString("description"), row.getString("period"),
                row.getObject("historical_year", Integer.class), row.getString("place"), row.getString("source"),
                row.getString("credits"), row.getString("usage_terms"), row.getObject("latitude", Double.class),
                row.getObject("longitude", Double.class), imageUrl(row.getObject("cover_id", Long.class)), List.of());
    }

    private static void exact(List<String> conditions, Map<String, Object> parameters, String column, String value) {
        if (!value.isBlank()) {
            conditions.add("m." + column + "=:" + column);
            parameters.put(column, value);
        }
    }

    private static String pattern(String value) {
        return "%" + value.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static String imageUrl(Long id) { return id == null ? null : "/api/files/" + id; }
}
