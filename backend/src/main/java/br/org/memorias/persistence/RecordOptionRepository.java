package br.org.memorias.persistence;

import br.org.memorias.domain.RecordOption;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RecordOptionRepository {
    private final JdbcClient db;

    public RecordOptionRepository(JdbcClient db) { this.db = db; }

    public List<RecordOption> list() {
        return db.sql("select id, kind, value from record_options order by kind, lower(value), value, id")
                .query((row, index) -> new RecordOption(row.getLong("id"), row.getString("kind"), row.getString("value")))
                .list();
    }

    public long create(RecordOption.Input input) {
        var key = new GeneratedKeyHolder();
        db.sql("insert into record_options (kind, value) values (:kind, :value)")
                .param("kind", input.kind()).param("value", input.value()).update(key, "id");
        return key.getKey().longValue();
    }

    public boolean update(long id, RecordOption.Input input) {
        return db.sql("update record_options set kind=:kind, value=:value where id=:id")
                .param("kind", input.kind()).param("value", input.value()).param("id", id).update() == 1;
    }

    public boolean delete(long id) {
        return db.sql("delete from record_options where id=:id").param("id", id).update() == 1;
    }
}
