package br.org.memorias.storage;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
class FileStorageConfiguration {
    @Bean
    FileStorage storage(@Value("${memorias.storage.endpoint}") String endpoint,
            @Value("${memorias.storage.access-key:}") String access,
            @Value("${memorias.storage.secret-key:}") String secret,
            @Value("${memorias.storage.bucket}") String bucket,
            @Value("${memorias.storage.directory:./local-data/files}") String directory) throws Exception {
        if (endpoint.isBlank()) {
            return new LocalFileStorage(Path.of(directory));
        }
        return new S3FileStorage(MinioClient.builder().endpoint(endpoint).credentials(access, secret).build(), bucket);
    }
}
