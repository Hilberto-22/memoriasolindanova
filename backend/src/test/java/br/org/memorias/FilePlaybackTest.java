package br.org.memorias;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = {
                "memorias.admin-user=admin", "memorias.admin-password=development-test-password",
                "memorias.storage.endpoint="
})
@ContextConfiguration(initializers = PostgreSqlTestSchema.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
class FilePlaybackTest {
        @TempDir
        static Path storage;
        @Autowired
        MockMvc mvc;
        @Autowired
        ObjectMapper json;

        @DynamicPropertySource
        static void properties(DynamicPropertyRegistry properties) {
                properties.add("memorias.storage.directory", () -> storage.toString());
        }

        @Test
        void servesAllPublishedAttachmentsAndVideoByteRangesWhileProtectingDrafts() throws Exception {
                var csrf = csrf(null);
                var login = mvc.perform(post("/api/session").param("username", "admin")
                                .param("password", "development-test-password").cookie(csrf)
                                .header("X-XSRF-TOKEN", csrf.getValue()))
                                .andExpect(status().isNoContent()).andReturn();
                var session = (MockHttpSession) login.getRequest().getSession(false);
                csrf = csrf(session);
                byte[] video = new byte[64];
                for (int index = 0; index < video.length; index++)
                        video[index] = (byte) index;
                System.arraycopy("ftypisom".getBytes(StandardCharsets.US_ASCII), 0, video, 4, 8);
                var draft = mvc.perform(multipart("/api/admin/drafts")
                                .file(new MockMultipartFile("memory", "", "application/json", """
                                                {"type":"Vídeo","collection":"cidade","title":"Registro multimídia"}
                                                """.getBytes(StandardCharsets.UTF_8)))
                                .file(new MockMultipartFile("files", "video.mp4", "video/mp4", video))
                                .file(new MockMultipartFile("files", "audio.mp3", "audio/mpeg",
                                                "ID3 registro de teste".getBytes(StandardCharsets.UTF_8)))
                                .file(new MockMultipartFile("files", "documento.pdf", "application/pdf",
                                                "%PDF-1.7 arquivo de teste".getBytes(StandardCharsets.UTF_8)))
                                .file(new MockMultipartFile("files", "foto.png", "image/png",
                                                Base64.getDecoder().decode(
                                                                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aIGsAAAAASUVORK5CYII=")))
                                .file(new MockMultipartFile("files", "outra-foto.jpg", "image/jpeg",
                                                new byte[] { (byte) 255, (byte) 216, (byte) 255, 0, 0, 0, 0, 0 }))
                                .session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                                .andExpect(status().isCreated()).andReturn();
                long draftId = json.readTree(draft.getResponse().getContentAsByteArray()).get("id").asLong();
                var review = mvc.perform(get("/api/admin/contributions").session(session)).andExpect(status().isOk())
                                .andReturn();
                var files = json.readTree(review.getResponse().getContentAsByteArray()).at("/0/files");
                long videoId = files.get(0).get("id").asLong();
                long audioId = files.get(1).get("id").asLong();
                long pdfId = files.get(2).get("id").asLong();
                mvc.perform(get("/api/files/{id}", videoId).header("Range", "bytes=0-15"))
                                .andExpect(status().isNotFound());
                mvc.perform(get("/api/admin/files/{id}", videoId).header("Range", "bytes=0-15"))
                                .andExpect(status().isUnauthorized());
                mvc.perform(get("/api/admin/files/{id}", videoId).session(session).header("Range", "bytes=0-15"))
                                .andExpect(status().isPartialContent())
                                .andExpect(content().bytes(Arrays.copyOfRange(video, 0, 16)));
                mvc.perform(patch("/api/admin/contributions/{id}", draftId).session(session).cookie(csrf)
                                .header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                                .content("""
                                                {"status":"APPROVED","category":"Vídeo","credits":"Equipe","usage":"Consulta"}
                                                """))
                                .andExpect(status().isNoContent());
                var catalog = mvc.perform(get("/api/memories").param("type", "Vídeo"))
                                .andExpect(jsonPath("$.total").value(1)).andReturn();
                long memoryId = json.readTree(catalog.getResponse().getContentAsByteArray()).at("/items/0/id").asLong();
                mvc.perform(get("/api/memories/{id}", memoryId)).andExpect(jsonPath("$.files.length()").value(5))
                                .andExpect(jsonPath("$.files[0].contentType").value("video/mp4"));

                mvc.perform(get("/api/files/{id}", videoId)).andExpect(status().isOk())
                                .andExpect(content().contentType("video/mp4")).andExpect(content().bytes(video))
                                .andExpect(header().string("Content-Disposition", startsWith("inline;")))
                                .andExpect(header().string("Content-Length", "64"))
                                .andExpect(header().string("Accept-Ranges", "bytes"));
                for (String range : new String[] { "bytes=0-15", "bytes=16-27", "bytes=20-", "bytes=-8",
                                "bytes=60-999" }) {
                        int start = switch (range) {
                                case "bytes=0-15" -> 0;
                                case "bytes=16-27" -> 16;
                                case "bytes=20-" -> 20;
                                case "bytes=-8" -> 56;
                                default -> 60;
                        };
                        int end = range.equals("bytes=0-15") ? 15 : range.equals("bytes=16-27") ? 27 : 63;
                        mvc.perform(get("/api/files/{id}", videoId).header("Range", range))
                                        .andExpect(status().isPartialContent())
                                        .andExpect(header().string("Content-Range",
                                                        "bytes " + start + "-" + end + "/64"))
                                        .andExpect(header().string("Content-Length", String.valueOf(end - start + 1)))
                                        .andExpect(content().bytes(Arrays.copyOfRange(video, start, end + 1)));
                }
                for (String invalid : new String[] { "bytes=64-", "bytes=1-0", "bytes=bad", "bytes=-0", "bytes=" }) {
                        mvc.perform(get("/api/files/{id}", videoId).header("Range", invalid))
                                        .andExpect(status().isRequestedRangeNotSatisfiable())
                                        .andExpect(header().string("Content-Range", "bytes */64"));
                }
                mvc.perform(get("/api/files/{id}", videoId).header("Range", "bytes=0-3,8-11"))
                                .andExpect(status().isOk()).andExpect(content().bytes(video));
                mvc.perform(get("/api/files/{id}", videoId).header("Range", "bytes=0-3").header("If-Range",
                                "\"old-version\""))
                                .andExpect(status().isOk()).andExpect(content().bytes(video));
                mvc.perform(head("/api/files/{id}", videoId)).andExpect(status().isOk())
                                .andExpect(header().string("Content-Length", "64"))
                                .andExpect(content().bytes(new byte[0]));
                mvc.perform(get("/api/files/{id}", videoId).param("download", "true"))
                                .andExpect(header().string("Content-Disposition", startsWith("attachment;")))
                                .andExpect(content().bytes(video));
                mvc.perform(get("/api/files/{id}", audioId)).andExpect(content().contentType("audio/mpeg"))
                                .andExpect(header().string("Content-Disposition", startsWith("inline;")));
                mvc.perform(get("/api/files/{id}", pdfId)).andExpect(content().contentType("application/pdf"))
                                .andExpect(header().string("Content-Disposition", startsWith("inline;")));
        }

        private Cookie csrf(MockHttpSession session) throws Exception {
                var request = get("/api/csrf");
                if (session != null)
                        request.session(session);
                return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
                                .getCookie("XSRF-TOKEN");
        }
}
