package br.org.memorias.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ContributionWriter {
    private final JdbcClient db;
    ContributionWriter(JdbcClient db) { this.db = db; }

    public long insert(String collection, String name, String email, String title, String type,
                       String description, String period, String place, String source, Long related) {
        var key = new GeneratedKeyHolder();
        db.sql("""
                insert into contributions
                (collection_slug,name,email,title,material_type,description,period,place,source,consent,related_memory)
                values (:collection,:name,:email,:title,:type,:description,:period,:place,:source,true,:related)
                """).param("collection", collection).param("name", name).param("email", email)
                .param("title", title).param("type", type).param("description", description)
                .param("period", period).param("place", place).param("source", source)
                .param("related", related, java.sql.Types.BIGINT).update(key, "id");
        return key.getKey().longValue();
    }

    public boolean decide(long id, String administrator, String decision) {
        return db.sql("""
                update contributions set status=:status, reviewed_at=current_timestamp, reviewed_by=:reviewer
                where id=:id and status='PENDING'
                """).param("status", decision).param("reviewer", administrator).param("id", id).update() == 1;
    }
}
