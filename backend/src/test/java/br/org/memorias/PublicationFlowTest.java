package br.org.memorias;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "memorias.admin-user=admin",
        "memorias.admin-password=development-test-password",
        "memorias.storage.endpoint="
})
@ContextConfiguration(initializers = PostgreSqlTestSchema.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class PublicationFlowTest {
    @TempDir
    static Path storage;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("memorias.storage.directory", () -> storage.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcClient db;

    @Test
    void submissionAndFilesBecomePublicOnlyAfterApproval() throws Exception {
        mvc.perform(get("/api/collections")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
        mvc.perform(get("/api/memories")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());

        Cookie anonymousCsrf = csrf(null);
        var submission = new MockMultipartFile("memory", "", "application/json", """
                {"name":"Pessoa de teste","email":"qa@example.org","type":"Documento","title":"Publicação de teste",
                 "description":"Registro efêmero de validação","period":"Anos 1990","place":"Olinda Nova",
                 "collection":"cidade","source":"Validação de desenvolvimento","consent":true}
                """.getBytes(StandardCharsets.UTF_8));
        var attachment = new MockMultipartFile("files", "teste.pdf", "application/pdf",
                "%PDF-1.4 arquivo de teste".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/contributions").file(submission).file(attachment)).andExpect(status().isForbidden());
        var receipt = mvc.perform(multipart("/api/contributions").file(submission).file(attachment)
                        .cookie(anonymousCsrf).header("X-XSRF-TOKEN", anonymousCsrf.getValue()))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(receipt.getResponse().getContentAsByteArray()).get("id").asLong();
        mvc.perform(get("/api/memories")).andExpect(jsonPath("$.total").value(0));

        var login = mvc.perform(post("/api/session").param("username", "admin")
                        .param("password", "development-test-password")
                        .cookie(anonymousCsrf).header("X-XSRF-TOKEN", anonymousCsrf.getValue()))
                .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/session").session(session)).andExpect(jsonPath("$.name").value("admin"));
        var dashboard = mvc.perform(get("/api/admin/dashboard").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(1)).andReturn();
        long fileId = json.readTree(dashboard.getResponse().getContentAsByteArray())
                .at("/recent/0/files/0/id").asLong();
        mvc.perform(get("/api/files/{id}", fileId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/files/{id}", fileId).session(session)).andExpect(status().isOk());

        Cookie adminCsrf = csrf(session);
        String review = """
                {"status":"APPROVED","category":"Teste","credits":"Teste","usage":"Teste",
                 "year":1993,"latitude":-2.99,"longitude":-44.99}
                """;
        mvc.perform(patch("/api/admin/contributions/{id}", id).session(session)
                        .cookie(adminCsrf).header("X-XSRF-TOKEN", adminCsrf.getValue())
                        .contentType("application/json").content(review)).andExpect(status().isNoContent());
        mvc.perform(patch("/api/admin/contributions/{id}", id).session(session)
                        .cookie(adminCsrf).header("X-XSRF-TOKEN", adminCsrf.getValue())
                        .contentType("application/json").content(review)).andExpect(status().isConflict());

        var records = mvc.perform(get("/api/memories").param("q", "publicação")
                        .param("collection", "cidade").param("year", "anos 1990"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andReturn();
        long memoryId = json.readTree(records.getResponse().getContentAsByteArray()).at("/items/0/id").asLong();
        mvc.perform(get("/api/memories/{id}", memoryId)).andExpect(jsonPath("$.files.length()").value(1));
        mvc.perform(get("/api/files/{id}", fileId)).andExpect(status().isOk());
        mvc.perform(get("/api/map")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/timeline")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/collections")).andExpect(jsonPath("$[0].count").value(1));
        verifyEditorialPublishing(session, adminCsrf, submission, review, memoryId);
        verifyOptionalDraftFields(session, adminCsrf);
        verifyEditingAndRemoval(session, adminCsrf, memoryId, fileId);
        mvc.perform(post("/api/logout").session(session).cookie(adminCsrf)
                .header("X-XSRF-TOKEN", adminCsrf.getValue())).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    private void verifyOptionalDraftFields(MockHttpSession session, Cookie csrf) throws Exception {
        String minimal = """
                {"type":"Documento","collection":"cidade","title":"Registro mínimo"}
                """;
        var memory = new MockMultipartFile("memory", "", "application/json", minimal.getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/admin/drafts").file(memory).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/record-options")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/contributions").file(memory).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isBadRequest());

        for (String field : new String[] {"type", "collection", "title"}) {
            var invalid = json.readTree(minimal).deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) invalid).put(field, "   ");
            mvc.perform(multipart("/api/admin/drafts")
                    .file(new MockMultipartFile("memory", "", "application/json", json.writeValueAsBytes(invalid)))
                    .session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                    .andExpect(status().isBadRequest());
        }
        for (String invalid : new String[] {
                minimal.replace("Documento", "Inválido"),
                minimal.replace("cidade", "inexistente"),
                minimal.replace("\"type\":", "\"email\":\"inválido\",\"type\":"),
                minimal.replace("\"type\":", "\"source\":\"" + "a".repeat(501) + "\",\"type\":") }) {
            mvc.perform(multipart("/api/admin/drafts")
                    .file(new MockMultipartFile("memory", "", "application/json", invalid.getBytes(StandardCharsets.UTF_8)))
                    .session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                    .andExpect(status().isBadRequest());
        }
        var created = mvc.perform(multipart("/api/admin/drafts").file(memory).session(session)
                        .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsByteArray()).get("id").asLong();
        assertFalse(db.sql("select consent from contributions where id=:id").param("id", id).query(Boolean.class).single());
        mvc.perform(patch("/api/admin/contributions/{id}", id).session(session).cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                        .content("""
                                {"status":"APPROVED","category":"Documento","credits":"Equipe","usage":"Consulta"}
                                """))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/memories").param("q", "Registro mínimo"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].description").value(""))
                .andExpect(jsonPath("$.items[0].source").value(""))
                .andExpect(jsonPath("$.items[0].place").value(""))
                .andExpect(jsonPath("$.items[0].files.length()").value(0));

        String suggested = """
                {"type":"Fotografia","collection":"cidade","title":"Com sugestões",
                 "place":" Praça da Matriz ","source":" Acervo da família ",
                 "name":null,"email":"","description":null,"consent":false}
                """;
        // Free-form values remain valid without changing the managed suggestion lists.
        for (int index = 0; index < 3; index++) {
            String payload = index == 2 ? suggested.replace("Praça da Matriz", "Local recusado")
                    .replace("Acervo da família", "Origem recusada") : suggested;
            var draft = mvc.perform(multipart("/api/admin/drafts")
                            .file(new MockMultipartFile("memory", "", "application/json", payload.getBytes(StandardCharsets.UTF_8)))
                            .session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                    .andExpect(status().isCreated()).andReturn();
            long draftId = json.readTree(draft.getResponse().getContentAsByteArray()).get("id").asLong();
            if (index == 2) {
                mvc.perform(patch("/api/admin/contributions/{id}", draftId).session(session).cookie(csrf)
                                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                                .content("{\"status\":\"REJECTED\"}"))
                        .andExpect(status().isNoContent());
            }
        }
        mvc.perform(get("/api/admin/record-options").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0))
                .andExpect(jsonPath("$.sources.length()").value(0));
        mvc.perform(get("/api/admin/dashboard").session(session))
                .andExpect(jsonPath("$.collaborators").value(1));
    }

    private void verifyEditorialPublishing(MockHttpSession session, Cookie csrf, MockMultipartFile submission,
                                           String review, long documentId) throws Exception {
        String cover = """
                {"slug":"fotografias","title":"Coleção de fotografias","description":"Uma seleção do acervo","coverMemory":%d}
                """;
        mvc.perform(post("/api/admin/collections").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(cover.formatted(documentId))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/collections").session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(cover.formatted(documentId))).andExpect(status().isBadRequest());

        var photo = new MockMultipartFile("files", "foto.png", "image/png", Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aIGsAAAAASUVORK5CYII="));
        var draft = mvc.perform(multipart("/api/admin/drafts").file(submission).file(photo).session(session)
                        .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isCreated()).andReturn();
        long draftId = json.readTree(draft.getResponse().getContentAsByteArray()).get("id").asLong();
        mvc.perform(get("/api/memories").param("withImage", "true")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(patch("/api/admin/contributions/{id}", draftId).session(session)
                        .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json").content(review)).andExpect(status().isNoContent());
        var photos = mvc.perform(get("/api/memories").param("withImage", "true"))
                .andExpect(jsonPath("$.total").value(1)).andReturn();
        var imageRecord = json.readTree(photos.getResponse().getContentAsByteArray()).at("/items/0");
        long photoId = imageRecord.get("id").asLong();
        String imageUrl = imageRecord.get("imageUrl").asText();

        mvc.perform(post("/api/admin/collections").session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(cover.formatted(photoId))).andExpect(status().isCreated());
        mvc.perform(get("/api/collections")).andExpect(jsonPath("$[6].imageUrl").value(imageUrl))
                .andExpect(jsonPath("$[6].coverMemory").value(photoId)).andExpect(jsonPath("$[6].count").value(0));
        mvc.perform(put("/api/admin/collections/fotografias").session(session).cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                        .content("""
                                {"title":"Fotografias selecionadas","description":"Seleção atualizada","coverMemory":null}
                                """)).andExpect(status().isNoContent());
        mvc.perform(get("/api/collections")).andExpect(jsonPath("$[6].title").value("Fotografias selecionadas"))
                .andExpect(jsonPath("$[6].imageUrl").doesNotExist());

        String editorial = """
                {"heroTitle":"Memórias da comunidade","heroDescription":"Fotografias do acervo",
                 "communityTitle":"Contribua","communityDescription":"Envie seu registro","featuredMemory":%d}
                """;
        mvc.perform(put("/api/admin/portal-content").session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(editorial.formatted(documentId))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/portal-content").session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(editorial.formatted(photoId))).andExpect(status().isNoContent());
        mvc.perform(get("/api/portal-content")).andExpect(jsonPath("$.content.heroTitle").value("Memórias da comunidade"))
                .andExpect(jsonPath("$.feature.imageUrl").value(imageUrl));
        mvc.perform(get(imageUrl)).andExpect(status().isOk());

        mvc.perform(put("/api/admin/memories/{id}", photoId).session(session).cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                        .content("""
                                {"title":"Fotografia identificada","category":"Fotografias","collectionSlug":"fotografias",
                                 "description":"Descrição atualizada","place":"Olinda Nova","source":"Teste",
                                 "credits":"Equipe do acervo","usage":"Teste","year":2002}
                                """)).andExpect(status().isNoContent());
        mvc.perform(get("/api/memories").param("q", "identificada").param("collection", "fotografias"))
                .andExpect(jsonPath("$.items[0].id").value(photoId));
        mvc.perform(get("/api/portal-content")).andExpect(jsonPath("$.feature.title").value("Fotografia identificada"));
        mvc.perform(get("/api/map")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/timeline")).andExpect(jsonPath("$[1].year").value(2002));
        verifyCityHistoryPublishing(session, csrf, photoId, imageUrl);
    }

    private void verifyCityHistoryPublishing(MockHttpSession session, Cookie csrf, long memoryId, String imageUrl) throws Exception {
        mvc.perform(get("/api/city-history")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        String payload = """
                {"title":"Símbolo de teste","category":"flag","content":"Descrição para validação.\\nSegunda linha.",
                 "period":"Anos 1990","startDate":"1990-01-01","endDate":"1999-12-31",
                 "source":"Documento de teste","memoryId":%d}
                """.formatted(memoryId);
        mvc.perform(post("/api/admin/city-history").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(payload)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/city-history").session(session)
                .contentType("application/json").content(payload)).andExpect(status().isForbidden());
        for (String invalid : new String[] {
                payload.replace("\"flag\"", "\"unknown\""),
                payload.replace("1999-12-31", "1989-12-31"),
                payload.replace("\"1990-01-01\"", "null"),
                payload.replace("\"1990-01-01\"", "\"0000-01-01\""),
                payload.replace("\"1990-01-01\"", "\"1990-02-30\""),
                payload.replace("\"Símbolo de teste\"", "\"   \""),
                payload.replace("\"memoryId\":" + memoryId, "\"memoryId\":9223372036854775807") }) {
            mvc.perform(post("/api/admin/city-history").session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                    .contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        }
        var created = mvc.perform(post("/api/admin/city-history").session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json").content(payload))
                .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsByteArray()).get("id").asLong();
        mvc.perform(get("/api/city-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].category").value("flag"))
                .andExpect(jsonPath("$[0].startDate").value("1990-01-01"))
                .andExpect(jsonPath("$[0].endDate").value("1999-12-31"))
                .andExpect(jsonPath("$[0].source").value("Documento de teste"))
                .andExpect(jsonPath("$[0].content").value("Descrição para validação.\nSegunda linha."))
                .andExpect(jsonPath("$[0].memoryTitle").value("Fotografia identificada"))
                .andExpect(jsonPath("$[0].imageUrl").value(imageUrl));
        String undated = """
                {"title":"Tradição de teste","category":"traditions","content":"Texto atualizado","period":"Tradição atual"}
                """;
        mvc.perform(put("/api/admin/city-history/{id}", id).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(undated)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/city-history/{id}", id).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(undated)).andExpect(status().isNoContent());
        mvc.perform(get("/api/city-history")).andExpect(jsonPath("$[0].title").value("Tradição de teste"))
                .andExpect(jsonPath("$[0].startDate").doesNotExist()).andExpect(jsonPath("$[0].memoryId").doesNotExist())
                .andExpect(jsonPath("$[0].imageUrl").doesNotExist());
        var dated = mvc.perform(post("/api/admin/city-history").session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json").content(payload))
                .andExpect(status().isCreated()).andReturn();
        long datedId = json.readTree(dated.getResponse().getContentAsByteArray()).get("id").asLong();
        mvc.perform(get("/api/city-history")).andExpect(jsonPath("$[0].id").value(datedId))
                .andExpect(jsonPath("$[1].id").value(id));
        mvc.perform(delete("/api/admin/city-history/{id}", id).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/city-history/{id}", id).session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/city-history/{id}", id).session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/city-history/{id}", id).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(undated)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/city-history/{id}", datedId).session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNoContent());
        mvc.perform(get("/api/city-history")).andExpect(jsonPath("$.length()").value(0));
    }

    private void verifyEditingAndRemoval(MockHttpSession session, Cookie csrf, long memoryId, long originalFile) throws Exception {
        String edit = """
                {"title":"Registro com novo arquivo","category":"Teste","collectionSlug":"cidade",
                 "description":"Descrição atualizada","source":"Teste","credits":"Equipe","usage":"Consulta"}
                """;
        var metadata = new MockMultipartFile("memory", "", "application/json", edit.getBytes(StandardCharsets.UTF_8));
        var pdf = new MockMultipartFile("files", "adicionado.pdf", "application/pdf",
                "%PDF-1.4 arquivo adicionado".getBytes(StandardCharsets.UTF_8));
        var invalid = new MockMultipartFile("files", "invalido.exe", "application/octet-stream", new byte[16]);
        String url = "/api/admin/memories/" + memoryId;

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, url).file(metadata).file(pdf)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isUnauthorized());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, url).file(metadata).file(pdf)
                .session(session)).andExpect(status().isForbidden());

        long storedBefore = storedFiles();
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, url).file(metadata).file(pdf).file(invalid)
                .session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/memories/{id}", memoryId))
                .andExpect(jsonPath("$.title").value("Publicação de teste"))
                .andExpect(jsonPath("$.files.length()").value(1));
        assertEquals(storedBefore, storedFiles(), "Failed editing must clean up newly stored files");

        var tooMany = multipart(org.springframework.http.HttpMethod.PUT, url).file(metadata);
        for (int i = 0; i < 6; i++) tooMany.file(pdf);
        mvc.perform(tooMany.session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/admin/memories/9223372036854775807")
                .file(metadata).file(pdf).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, url).file(metadata).file(pdf)
                .session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        var updated = mvc.perform(get("/api/memories/{id}", memoryId))
                .andExpect(jsonPath("$.title").value("Registro com novo arquivo"))
                .andExpect(jsonPath("$.files.length()").value(2)).andReturn();
        var files = json.readTree(updated.getResponse().getContentAsByteArray()).get("files");
        long newFile = files.get(1).get("id").asLong();
        mvc.perform(get("/api/files/{id}", originalFile)).andExpect(status().isOk());
        mvc.perform(get("/api/files/{id}", newFile)).andExpect(status().isOk());
        assertEquals(storedBefore + 1, storedFiles());

        long contribution = db.sql("select contribution_id from memories where id=:id")
                .param("id", memoryId).query(Long.class).single();
        db.sql("update portal_content set featured_memory=:id").param("id", memoryId).update();
        db.sql("update collections set cover_memory=:id where slug='cidade'").param("id", memoryId).update();
        db.sql("update contributions set related_memory=:id where id<>:contribution")
                .param("id", memoryId).param("contribution", contribution).update();
        db.sql("insert into city_history(title,category,content,memory_id) values ('Vínculo','history','Texto',:id)")
                .param("id", memoryId).update();

        mvc.perform(delete(url).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isUnauthorized());
        mvc.perform(delete(url).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/memories/{id}", memoryId)).andExpect(status().isOk());
        mvc.perform(delete(url).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/memories/{id}", memoryId)).andExpect(status().isNotFound());
        for (long file : new long[] {originalFile, newFile}) {
            mvc.perform(get("/api/files/{id}", file)).andExpect(status().isNotFound());
            mvc.perform(get("/api/admin/files/{id}", file).session(session)).andExpect(status().isNotFound());
        }
        assertEquals(storedBefore - 1, storedFiles(), "Deleting a record must remove all its stored files");
        assertEquals(0L, db.sql("select count(*) from contributions where id=:id").param("id", contribution).query(Long.class).single());
        assertEquals(0L, db.sql("select count(*) from contributions where related_memory=:id").param("id", memoryId).query(Long.class).single());
        mvc.perform(get("/api/collections")).andExpect(jsonPath("$[0].coverMemory").doesNotExist());
        mvc.perform(get("/api/portal-content")).andExpect(jsonPath("$.content.featuredMemory").doesNotExist());
        mvc.perform(get("/api/city-history"))
                .andExpect(jsonPath("$[0].title").value("Vínculo"))
                .andExpect(jsonPath("$[0].memoryId").doesNotExist());
        mvc.perform(delete(url).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());
    }

    private long storedFiles() throws Exception {
        try (var paths = Files.list(storage)) { return paths.count(); }
    }

    private Cookie csrf(MockHttpSession session) throws Exception {
        var request = get("/api/csrf");
        if (session != null) request.session(session);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}
