package br.org.memorias.web;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
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
    ResponseEntity<InputStreamResource> publicFile(@PathVariable long id,
            @RequestParam(defaultValue = "false") boolean download, HttpServletRequest request) {
        return file(id, false, download, request);
    }

    @GetMapping("/admin/files/{id}")
    ResponseEntity<InputStreamResource> privateFile(@PathVariable long id,
            @RequestParam(defaultValue = "false") boolean download, HttpServletRequest request) {
        return file(id, true, download, request);
    }

    private ResponseEntity<InputStreamResource> file(long id, boolean admin, boolean download, HttpServletRequest request) {
        // Check authorization before accessing metadata or content, including byte ranges.
        var file = found(archive.storedFile(id, admin));
        try {
            long size = storage.size(file.key());
            boolean head = request.getMethod().equals("HEAD");
            long start = 0;
            long end = size - 1;
            boolean partial = false;
            String range = request.getHeader(HttpHeaders.RANGE);
            if (!head && request.getHeader(HttpHeaders.IF_RANGE) == null && range != null && range.startsWith("bytes=")) {
                try {
                    var ranges = HttpRange.parseRanges(range);
                    if (ranges.isEmpty()) throw new IllegalArgumentException("Empty range");
                    // Multiple ranges may be ignored; media players request one range at a time.
                    if (ranges.size() == 1) {
                        start = ranges.getFirst().getRangeStart(size);
                        end = ranges.getFirst().getRangeEnd(size);
                        if (start >= size || end < start) throw new IllegalArgumentException("Unsatisfiable range");
                        partial = true;
                    }
                } catch (IllegalArgumentException failure) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + size)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes").build();
                }
            }
            long length = partial ? end - start + 1 : size;
            var disposition = download ? ContentDisposition.attachment() : ContentDisposition.inline();
            var response = ResponseEntity.status(partial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                    .contentType(MediaType.parseMediaType(file.type())).contentLength(length)
                    .cacheControl(CacheControl.noStore()).header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(file.name(), StandardCharsets.UTF_8).build().toString());
            if (partial) response.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + size);
            if (head) return response.build();
            var input = partial ? storage.openRange(file.key(), start, length) : storage.open(file.key());
            return response.body(new InputStreamResource(input));
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Arquivo indisponível.", error);
        }
    }

    private static <T> T found(T result) {
        return Optional.ofNullable(result).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
