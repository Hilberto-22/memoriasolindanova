package br.org.memorias.persistence;

import br.org.memorias.application.AttachmentService;
import br.org.memorias.storage.FileStorage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentUploadLimitTest {
    private static final long MB = 1024L * 1024;
    private static final byte[] MP4 = {0, 0, 0, 16, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};
    private static final byte[] PDF = "%PDF-1.7 test file".getBytes(StandardCharsets.US_ASCII);
    private final RecordingStorage storage = new RecordingStorage();
    private final RecordingAttachments attachments = new RecordingAttachments();
    private final AttachmentService service = new AttachmentService(attachments, storage);

    @Test
    void acceptsVideoAt300MbAndOtherFilesAt20Mb() {
        var keys = new ArrayList<String>();
        service.save(1, file("video.mp4", "video/mp4", MP4, 300 * MB), keys);
        assertEquals(300 * MB, storage.length);
        assertEquals("video/mp4", storage.type);
        service.save(1, file("documento.pdf", "application/pdf", PDF, 20 * MB), keys);
        assertEquals(20 * MB, storage.length);
        assertEquals("application/pdf", storage.type);
        assertEquals(2, attachments.inserted);
        assertEquals(2, keys.size());
    }

    @Test
    void rejectsOneByteAboveEachLimitBeforeStoring() {
        var keys = new ArrayList<String>();
        assertThrows(ResponseStatusException.class,
                () -> service.save(1, file("video.mp4", "video/mp4", MP4, 300 * MB + 1), keys));
        assertThrows(ResponseStatusException.class,
                () -> service.save(1, file("documento.pdf", "application/pdf", PDF, 20 * MB + 1), keys));
        assertEquals(0, storage.writes);
        assertEquals(0, attachments.inserted);
        assertTrue(keys.isEmpty());
    }

    @Test
    void videoLimitDependsOnContentRatherThanExtensionOrDeclaredType() {
        var keys = new ArrayList<String>();
        assertThrows(ResponseStatusException.class,
                () -> service.save(1, file("video.mp4", "video/mp4", PDF, 21 * MB), keys));
        service.save(1, file("video.bin", "application/octet-stream", MP4, 300 * MB), keys);
        assertEquals("video/mp4", storage.type);
        assertEquals(1, storage.writes);
    }

    @Test
    void rejectsEmptyAndUnsupportedFiles() {
        var keys = new ArrayList<String>();
        assertThrows(ResponseStatusException.class,
                () -> service.save(1, file("vazio.mp4", "video/mp4", new byte[0], 0), keys));
        assertThrows(ResponseStatusException.class,
                () -> service.save(1, file("falso.mp4", "video/mp4", PDF.clone(), 301 * MB), keys));
        assertThrows(ResponseStatusException.class,
                () -> service.save(1, file("falso.mp4", "video/mp4", new byte[16], 21 * MB), keys));
        assertEquals(0, storage.writes);
    }

    private MockMultipartFile file(String name, String type, byte[] header, long length) {
        // Exercise size boundaries without allocating a 300 MB test fixture.
        return new MockMultipartFile("files", name, type, header) {
            @Override public long getSize() { return length; }
        };
    }

    private static class RecordingAttachments extends AttachmentRepository {
        int inserted;
        RecordingAttachments() { super(null); }
        @Override public void insert(long id, String key, String name, String type) { inserted++; }
    }

    private static class RecordingStorage implements FileStorage {
        int writes;
        long length;
        String type;
        @Override public void put(String key, InputStream input, long length, String type) {
            writes++;
            this.length = length;
            this.type = type;
        }
        @Override public InputStream open(String key) { throw new UnsupportedOperationException(); }
        @Override public long size(String key) { throw new UnsupportedOperationException(); }
        @Override public InputStream openRange(String key, long offset, long length) { throw new UnsupportedOperationException(); }
        @Override public void remove(String key) { throw new UnsupportedOperationException(); }
    }
}
