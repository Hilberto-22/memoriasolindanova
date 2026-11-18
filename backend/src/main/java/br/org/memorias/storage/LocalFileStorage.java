package br.org.memorias.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class LocalFileStorage implements FileStorage {
    private final Path directory;

    LocalFileStorage(Path directory) throws IOException {
        this.directory = Files.createDirectories(directory);
    }

    @Override
    public void put(String key, InputStream input, long length, String type) throws IOException {
        Files.copy(input, directory.resolve(key));
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(directory.resolve(key));
    }

    @Override
    public void remove(String key) throws IOException {
        Files.deleteIfExists(directory.resolve(key));
    }
}
