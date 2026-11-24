package br.org.memorias.storage;

import com.sun.net.httpserver.HttpServer;
import io.minio.MinioClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class S3FileRangeTest {
    @Test
    void readsMetadataAndRequestsOnlyTheSelectedBytesFromMinio() throws Exception {
        byte[] bytes = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
        var requestedRange = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/memorias/video", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "video/mp4");
            exchange.getResponseHeaders().set("ETag", "\"test-etag\"");
            exchange.getResponseHeaders().set("Last-Modified", "Mon, 23 Nov 2026 12:00:00 GMT");
            if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(bytes.length));
                exchange.sendResponseHeaders(200, -1);
            } else {
                requestedRange.set(exchange.getRequestHeaders().getFirst("Range"));
                exchange.getResponseHeaders().set("Content-Range", "bytes 4-9/16");
                exchange.sendResponseHeaders(206, 6);
                exchange.getResponseBody().write(Arrays.copyOfRange(bytes, 4, 10));
            }
            exchange.close();
        });
        server.start();
        try (var client = MinioClient.builder().endpoint("http://127.0.0.1:" + server.getAddress().getPort())
                .region("us-east-1").build()) {
            var storage = new S3FileStorage(client, "memorias");
            assertEquals(16, storage.size("video"));
            try (var input = storage.openRange("video", 4, 6)) {
                assertArrayEquals(Arrays.copyOfRange(bytes, 4, 10), input.readAllBytes());
            }
            assertEquals("bytes=4-9", requestedRange.get());
        } finally {
            server.stop(0);
        }
    }
}
