package br.org.memorias.persistence;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.org.memorias.domain.ArchiveModels.*;
import br.org.memorias.domain.ArchiveModels.MemoryFile;
import br.org.memorias.domain.ArchiveModels.StoredFile;

@Repository
public class AttachmentRepository {
    private final JdbcClient db;

    AttachmentRepository(JdbcClient db) {
        this.db = db;
    }

    public StoredFile storedFile(long id, boolean admin) {
        String access = admin ? "" : " and exists (select 1 from memories m where m.contribution_id=f.contribution_id)";
        return db.sql("select f.* from memory_files f where f.id=:id" + access).param("id", id)
                .query((row, index) -> new StoredFile(row.getString("storage_key"),
                        row.getString("file_name"), row.getString("content_type")))
                .optional().orElse(null);
    }

    public List<MemoryFile> files(long contribution, boolean admin) {
        String prefix = admin ? "/api/admin/files/" : "/api/files/";
        return db.sql("select * from memory_files where contribution_id=:id order by id")
                .param("id", contribution).query((row, index) -> {
                    long id = row.getLong("id");
                    return new MemoryFile(id, row.getString("file_name"), row.getString("content_type"), prefix + id);
                }).list();
    }

    public void insert(long id, String key, String name, String type) {
        db.sql("insert into memory_files(contribution_id,storage_key,file_name,content_type) values (:id,:key,:name,:type)")
                .param("id", id).param("key", key).param("name", name).param("type", type).update();
    }

}
