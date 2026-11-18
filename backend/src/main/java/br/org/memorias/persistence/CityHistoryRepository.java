package br.org.memorias.persistence;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.org.memorias.domain.CityHistory;

@Repository
public class CityHistoryRepository {
    private final JdbcClient db;

    public CityHistoryRepository(JdbcClient db) { this.db = db; }

    public List<CityHistory.Entry> list() {
        return db.sql("""
                select h.*, m.title as memory_title,
                  (select min(f.id) from memory_files f where f.contribution_id=m.contribution_id
                    and f.content_type like 'image/%') as cover_id
                from city_history h left join memories m on m.id=h.memory_id
                order by h.start_date asc nulls last, h.id
                """).query((row, index) -> {
                    Long cover = row.getObject("cover_id", Long.class);
                    return new CityHistory.Entry(row.getLong("id"), row.getString("title"),
                            row.getString("category"), row.getString("content"), row.getString("period"),
                            row.getObject("start_date", LocalDate.class), row.getObject("end_date", LocalDate.class),
                            row.getString("source"), row.getObject("memory_id", Long.class),
                            row.getString("memory_title"), cover == null ? null : "/api/files/" + cover);
                }).list();
    }

    public long create(CityHistory.Input input) {
        return bind(db.sql("""
                insert into city_history (title, category, content, period, start_date, end_date, source, memory_id)
                values (:title, :category, :content, :period, :start, :end, :source, :memory) returning id
                """), input).query(Long.class).single();
    }

    public boolean update(long id, CityHistory.Input input) {
        return bind(db.sql("""
                update city_history set title=:title, category=:category, content=:content, period=:period,
                  start_date=:start, end_date=:end, source=:source, memory_id=:memory where id=:id
                """), input).param("id", id).update() == 1;
    }

    public boolean delete(long id) {
        return db.sql("delete from city_history where id=:id").param("id", id).update() == 1;
    }

    private JdbcClient.StatementSpec bind(JdbcClient.StatementSpec sql, CityHistory.Input input) {
        return sql.param("title", input.title()).param("category", input.category()).param("content", input.content())
                .param("period", input.period()).param("start", input.startDate(), Types.DATE)
                .param("end", input.endDate(), Types.DATE).param("source", input.source())
                .param("memory", input.memoryId(), Types.BIGINT);
    }
}
