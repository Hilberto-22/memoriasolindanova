package br.org.memorias.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PublicationRepository {
    private final JdbcClient db;
    public PublicationRepository(JdbcClient db) { this.db = db; }

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
