package br.org.memorias;

import br.org.memorias.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "memorias.admin-password=default-profile-test-password",
        "memorias.storage.endpoint="
})
@ContextConfiguration(initializers = PostgreSqlTestSchema.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DefaultProfileStartupTest {
    @TempDir
    static Path directory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("STORAGE_DIRECTORY", () -> directory.toString());
    }

    @Autowired FileStorage storage;

    @Test
    void startsWithoutDevProfileAndStoresFilesLocally() throws Exception {
        byte[] content = "Registro de teste".getBytes(StandardCharsets.UTF_8);
        storage.put("startup.txt", new ByteArrayInputStream(content), content.length, "text/plain");

        assertThat(Files.readAllBytes(directory.resolve("startup.txt"))).isEqualTo(content);
        try (var input = storage.open("startup.txt")) {
            assertThat(input.readAllBytes()).isEqualTo(content);
        }

        storage.remove("startup.txt");
        assertThat(directory.resolve("startup.txt")).doesNotExist();
    }
}
