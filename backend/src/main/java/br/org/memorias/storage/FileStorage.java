package br.org.memorias.storage;

import java.io.InputStream;

public interface FileStorage {
    void put(String key, InputStream input, long length, String type) throws Exception;

    InputStream open(String key) throws Exception;

    void remove(String key) throws Exception;
}
