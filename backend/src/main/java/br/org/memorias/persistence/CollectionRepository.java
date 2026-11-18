package br.org.memorias.persistence;

import java.util.List;
import java.sql.Types;
import br.org.memorias.domain.ArchiveModels.Collection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CollectionRepository {
    private final JdbcClient db;

    CollectionRepository(JdbcClient db) {
        this.db = db;
    }

    public boolean exists(String slug) {
        return db.sql("select count(*) from collections where slug=:slug").param("slug", slug)
                .query(Long.class).single() > 0;
    }

    public List<Collection> collections() {
        return db.sql("""
                select c.slug, c.title, c.description, c.cover_memory, count(m.id) as total,
                  coalesce((select min(f.id) from memory_files f join memories cover
                    on cover.contribution_id=f.contribution_id
                    where cover.id=c.cover_memory and f.content_type like 'image/%'),
                  (select min(f.id) from memory_files f join memories published
                   on published.contribution_id=f.contribution_id
                   where published.collection_slug=c.slug and f.content_type like 'image/%')) as cover_id
                from collections c left join memories m on c.slug=m.collection_slug
                group by c.slug, c.title, c.description, c.cover_memory, c.display_order order by c.display_order
                """).query((row, index) -> new Collection(row.getString("slug"), row.getString("title"),
                row.getLong("total"), imageUrl(row.getObject("cover_id", Long.class)), row.getString("description"),
                row.getObject("cover_memory", Long.class))).list();
    }

    public void create(String slug, String title, String description, Long coverMemory) {
        db.sql("""
                insert into collections(slug,title,description,display_order,cover_memory)
                values (:slug,:title,:description,nextval('collection_display_order'),:coverMemory)
                """).param("slug",slug).param("title",title).param("description",description)
                .param("coverMemory", coverMemory, Types.BIGINT).update();
    }

    public boolean update(String slug, String title, String description, Long coverMemory) {
        return db.sql("update collections set title=:title, description=:description, cover_memory=:coverMemory where slug=:slug")
                .param("slug",slug).param("title",title).param("description",description)
                .param("coverMemory", coverMemory, Types.BIGINT).update() == 1;
    }

    private static String imageUrl(Long id) { return id == null ? null : "/api/files/" + id; }
}
