package br.org.memorias.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import java.io.InputStream;

record S3FileStorage(MinioClient client, String bucket) implements FileStorage {
    @Override
    public void put(String key, InputStream input, long length, String type) throws Exception {
        client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType(type)
                .stream(input, length, -1).build());
    }

    @Override
    public InputStream open(String key) throws Exception {
        return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
    }

    @Override
    public long size(String key) throws Exception {
        return client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build()).size();
    }

    @Override
    public InputStream openRange(String key, long offset, long length) throws Exception {
        return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).offset(offset).length(length).build());
    }

    @Override
    public void remove(String key) throws Exception {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    }
}
