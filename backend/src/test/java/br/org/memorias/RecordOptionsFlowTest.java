package br.org.memorias;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "memorias.admin-user=admin",
        "memorias.admin-password=development-test-password",
        "memorias.storage.endpoint="
})
@ContextConfiguration(initializers = PostgreSqlTestSchema.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class RecordOptionsFlowTest {
    private static final String ENTRIES = "/api/admin/record-options/entries";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Environment environment;

    @Test
    void migrationImportsExistingSuggestionsWithoutDuplicatesOrRejectedValues() throws Exception {
        String url = environment.getRequiredProperty("spring.datasource.url");
        String user = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schemaName = "memorias_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var schema = new PostgreSqlTestSchema.TestSchema(url, user, password, schemaName)) {
            schema.create();
            Flyway.configure().dataSource(url, user, password).defaultSchema(schemaName).schemas(schemaName)
                    .target("6").load().migrate();
            try (var connection = DriverManager.getConnection(url, user, password)) {
                connection.setSchema(schemaName);
                var db = JdbcClient.create(new SingleConnectionDataSource(connection, true));
                db.sql("""
                        insert into contributions
                        (collection_slug,name,email,title,material_type,description,period,place,source,consent,status)
                        values
                        ('cidade','','','Pendente','Documento','','',' Praça da Matriz ',' Acervo da família ',false,'PENDING'),
                        ('cidade','','','Repetido','Documento','','','praça da matriz','acervo da família',false,'PENDING'),
                        ('cidade','','','Vazio','Documento','','','   ','',false,'PENDING'),
                        ('cidade','','','Recusado','Documento','','','Local recusado','Origem recusada',false,'REJECTED'),
                        ('cidade','','','Publicado','Documento','','','Local antigo','Origem antiga',false,'APPROVED')
                        """).update();
                db.sql("""
                        insert into memories
                        (contribution_id,collection_slug,title,material_type,category,description,period,place,source,credits,usage_terms)
                        select id,collection_slug,title,material_type,'Documento','','','Centro','Arquivo público','',''
                        from contributions where status='APPROVED'
                        """).update();
                Flyway.configure().dataSource(url, user, password).defaultSchema(schemaName).schemas(schemaName)
                        .load().migrate();
                assertEquals(4, db.sql("select count(*) from record_options").query(Integer.class).single());
                assertEquals(2, db.sql("select count(*) from record_options where kind='PLACE'").query(Integer.class).single());
                assertEquals(2, db.sql("select count(*) from record_options where kind='SOURCE'").query(Integer.class).single());
                assertEquals(0, db.sql("select count(*) from record_options where value like '%recusad%' or value like '%antig%'")
                        .query(Integer.class).single());
                assertEquals(2, db.sql("select count(*) from record_options where value in ('Centro','Arquivo público')")
                        .query(Integer.class).single());
            }
        }
    }

    @Test
    void managesOptionsAndKeepsPreviouslySavedRecords() throws Exception {
        Cookie csrf = csrf(null);
        String place = "{\"kind\":\"PLACE\",\"value\":\" Praça da Matriz \"}";
        mvc.perform(get(ENTRIES)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/record-options")).andExpect(status().isUnauthorized());
        mvc.perform(post(ENTRIES).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(place)).andExpect(status().isUnauthorized());
        mvc.perform(put(ENTRIES + "/1").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(place)).andExpect(status().isUnauthorized());
        mvc.perform(delete(ENTRIES + "/1").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isUnauthorized());

        var login = mvc.perform(post("/api/session").param("username", "admin")
                        .param("password", "development-test-password")
                        .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        csrf = csrf(session);
        mvc.perform(post(ENTRIES).session(session).contentType("application/json").content(place))
                .andExpect(status().isForbidden());
        mvc.perform(get(ENTRIES).session(session)).andExpect(jsonPath("$.length()").value(0));

        long placeId = create(session, csrf, place);
        long sourceId = create(session, csrf, "{\"kind\":\"SOURCE\",\"value\":\"Acervo da família\"}");
        mvc.perform(get(ENTRIES).session(session)).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(placeId))
                .andExpect(jsonPath("$[0].value").value("Praça da Matriz"));
        mvc.perform(get("/api/admin/record-options").session(session))
                .andExpect(jsonPath("$.places[0]").value("Praça da Matriz"))
                .andExpect(jsonPath("$.sources[0]").value("Acervo da família"));
        mvc.perform(post(ENTRIES).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json").content("{\"kind\":\"PLACE\",\"value\":\" praça da matriz \"}"))
                .andExpect(status().isConflict());

        for (String invalid : new String[] {
                "{}", "{\"kind\":\"INVALID\",\"value\":\"Teste\"}",
                "{\"kind\":\"PLACE\",\"value\":null}", "{\"kind\":\"PLACE\",\"value\":\"   \"}",
                "{\"kind\":\"PLACE\",\"value\":\"" + "a".repeat(181) + "\"}",
                "{\"kind\":\"SOURCE\",\"value\":\"" + "a".repeat(501) + "\"}" }) {
            mvc.perform(post(ENTRIES).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                    .contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        }
        long anotherPlace = create(session, csrf, "{\"kind\":\"PLACE\",\"value\":\"Outro local\"}");
        mvc.perform(put(ENTRIES + "/{id}", anotherPlace).session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json").content(place))
                .andExpect(status().isConflict());
        // The same name is allowed in different lists, and the full source limit is supported.
        long sameName = create(session, csrf, "{\"kind\":\"SOURCE\",\"value\":\"Praça da Matriz\"}");
        long longSource = create(session, csrf, "{\"kind\":\"SOURCE\",\"value\":\"" + "a".repeat(500) + "\"}");
        for (long id : new long[] {anotherPlace, sameName, longSource}) {
            mvc.perform(delete(ENTRIES + "/{id}", id).session(session).cookie(csrf)
                    .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNoContent());
        }

        var draft = mvc.perform(multipart("/api/admin/drafts")
                        .file(new MockMultipartFile("memory", "", "application/json", """
                                {"type":"Documento","collection":"cidade","title":"Registro com opções",
                                 "place":"Praça da Matriz","source":"Acervo da família"}
                                """.getBytes(StandardCharsets.UTF_8)))
                        .session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isCreated()).andReturn();
        long draftId = json.readTree(draft.getResponse().getContentAsByteArray()).get("id").asLong();
        mvc.perform(patch("/api/admin/contributions/{id}", draftId).session(session).cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json").content("""
                                {"status":"APPROVED","category":"Documento","credits":"Equipe","usage":"Consulta"}
                                """))
                .andExpect(status().isNoContent());
        mvc.perform(put(ENTRIES + "/{id}", placeId).session(session).cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                        .content("{\"kind\":\"PLACE\",\"value\":\"Largo da Matriz\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(delete(ENTRIES + "/{id}", sourceId).session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/record-options").session(session))
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0]").value("Largo da Matriz"))
                .andExpect(jsonPath("$.sources.length()").value(0));
        mvc.perform(get("/api/memories")).andExpect(jsonPath("$.items[0].place").value("Praça da Matriz"))
                .andExpect(jsonPath("$.items[0].source").value("Acervo da família"));
        mvc.perform(get("/api/admin/contributions").session(session))
                .andExpect(jsonPath("$[0].place").value("Praça da Matriz"))
                .andExpect(jsonPath("$[0].source").value("Acervo da família"));

        mvc.perform(delete(ENTRIES + "/{id}", sourceId).session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())).andExpect(status().isNotFound());
        mvc.perform(put(ENTRIES + "/{id}", sourceId).session(session).cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json").content(place))
                .andExpect(status().isNotFound());
    }

    private long create(MockHttpSession session, Cookie csrf, String payload) throws Exception {
        var result = mvc.perform(post(ENTRIES).session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(payload)).andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("id").asLong();
    }

    private Cookie csrf(MockHttpSession session) throws Exception {
        var request = get("/api/csrf");
        if (session != null) request.session(session);
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}
