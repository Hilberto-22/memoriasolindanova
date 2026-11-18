package br.org.memorias.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.org.memorias.domain.PortalContent;

@Repository
public class PortalContentRepository {
    private final JdbcClient db;
    public PortalContentRepository(JdbcClient db) { this.db = db; }

    public PortalContent read() {
        return db.sql("select * from portal_content where id=1").query((row,index) -> new PortalContent(
                row.getString("hero_title"),row.getString("hero_description"),row.getString("community_title"),
                row.getString("community_description"),row.getString("about"),row.getString("guardians"),
                row.getString("contact_email"),row.getObject("featured_memory", Long.class))).single();
    }

    public void save(PortalContent input) {
        db.sql("""
                update portal_content set hero_title=:title, hero_description=:description, community_title=:community,
                community_description=:communityDescription, about=:about, guardians=:guardians,
                contact_email=:email, featured_memory=:featured where id=1
                """).param("title",input.heroTitle()).param("description",input.heroDescription())
                .param("community",input.communityTitle()).param("communityDescription",input.communityDescription())
                .param("about",input.about()).param("guardians",input.guardians()).param("email",input.contactEmail())
                .param("featured",input.featuredMemory(), java.sql.Types.BIGINT).update();
    }
}
