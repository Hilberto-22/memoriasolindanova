package br.org.memorias.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import br.org.memorias.persistence.AttachmentRepository;
import br.org.memorias.storage.FileStorage;
@RestController
@RequestMapping("/api")
class FileController {
    private final AttachmentRepository archive;
    private final FileStorage storage;

    FileController(AttachmentRepository archive, FileStorage storage) {
        this.archive = archive;
        this.storage = storage;
    }
    @GetMapping("/files/{id}")
    ResponseEntity<InputStreamResource> publicFile(@PathVariable long id) { return file(id, false); }

    @GetMapping("/admin/files/{id}")
    ResponseEntity<InputStreamResource> privateFile(@PathVariable long id) { return file(id, true); }

    private ResponseEntity<InputStreamResource> file(long id, boolean admin) {
        var file = found(archive.storedFile(id, admin));
        try {
            var content = new InputStreamResource(storage.open(file.key()));
            var disposition = file.type().startsWith("image/") ? ContentDisposition.inline() : ContentDisposition.attachment();
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.type()))
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(file.name(), StandardCharsets.UTF_8).build().toString())
                    .body(content);
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Arquivo indisponível.", error);
        }
    }

    private static <T> T found(T result) {
        return Optional.ofNullable(result).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
