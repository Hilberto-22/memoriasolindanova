package br.org.memorias.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PublicationRepository {
    private final JdbcClient db;
    public PublicationRepository(JdbcClient db) { this.db = db; }

    public Optional<Long> contributionForUpdate(long id) {
        return db.sql("""
                select c.id from contributions c join memories m on m.contribution_id=c.id
                where m.id=:id for update of c, m
                """).param("id", id).query(Long.class).optional();
    }

    public List<String> delete(long id, long contribution) {
        var keys = db.sql("select storage_key from memory_files where contribution_id=:id")
                .param("id", contribution).query(String.class).list();
        db.sql("update portal_content set featured_memory=null where featured_memory=:id").param("id", id).update();
        db.sql("update city_history set memory_id=null where memory_id=:id").param("id", id).update();
        db.sql("update contributions set related_memory=null where related_memory=:id").param("id", id).update();
        db.sql("delete from memory_files where contribution_id=:id").param("id", contribution).update();
        db.sql("delete from memories where id=:id").param("id", id).update();
        db.sql("delete from contributions where id=:id").param("id", contribution).update();
        return keys;
    }

    public void publish(long id, String category, String credits, String usage, Integer year, Double lat, Double lon) {
        db.sql("""
                insert into memories (contribution_id,collection_slug,title,material_type,category,description,
                  period,historical_year,place,source,credits,usage_terms,latitude,longitude)
                select id,collection_slug,title,material_type,:category,description,period,:year,place,source,
                  :credits,:usage,:latitude,:longitude from contributions where id=:id
                """).param("id", id).param("category", category).param("credits", credits)
                .param("usage", usage).param("year", year, java.sql.Types.INTEGER)
                .param("latitude", lat, java.sql.Types.DOUBLE).param("longitude", lon, java.sql.Types.DOUBLE).update();
    }

    public boolean update(long id, String title, String category, String collection, String description, String period,
                          String place, String source, String credits, String usage, Integer year, Double latitude, Double longitude) {
        return db.sql("""
                update memories set title=:title, category=:category, collection_slug=:collection,
                    description=:description, period=:period, place=:place, source=:source, credits=:credits,
                    usage_terms=:usage, historical_year=:year, latitude=:latitude, longitude=:longitude
                where id=:id
                """).param("id", id).param("title", title).param("category", category).param("collection", collection)
                .param("description", description).param("period", period).param("place", place).param("source", source)
                .param("credits", credits).param("usage", usage).param("year", year, java.sql.Types.INTEGER)
                .param("latitude", latitude, java.sql.Types.DOUBLE).param("longitude", longitude, java.sql.Types.DOUBLE)
                .update() == 1;
    }
}
