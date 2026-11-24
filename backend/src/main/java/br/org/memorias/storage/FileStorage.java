package br.org.memorias.storage;

import java.io.InputStream;

public interface FileStorage {
    void put(String key, InputStream input, long length, String type) throws Exception;

    InputStream open(String key) throws Exception;

    long size(String key) throws Exception;

    InputStream openRange(String key, long offset, long length) throws Exception;

    void remove(String key) throws Exception;
}
