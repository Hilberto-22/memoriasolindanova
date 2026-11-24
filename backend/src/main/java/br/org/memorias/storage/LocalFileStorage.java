package br.org.memorias.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

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
    public long size(String key) throws IOException {
        return Files.size(directory.resolve(key));
    }

    @Override
    public InputStream openRange(String key, long offset, long length) throws IOException {
        var channel = FileChannel.open(directory.resolve(key), StandardOpenOption.READ);
        try {
            channel.position(offset);
            return new LimitedInputStream(Channels.newInputStream(channel), length);
        } catch (IOException | RuntimeException failure) {
            channel.close();
            throw failure;
        }
    }

    @Override
    public void remove(String key) throws IOException {
        Files.deleteIfExists(directory.resolve(key));
    }
}
