package br.org.memorias.storage;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Keeps a local file response within the requested byte range. */
final class LimitedInputStream extends FilterInputStream {
    private long remaining;

    LimitedInputStream(InputStream input, long length) {
        super(input);
        remaining = length;
    }

    @Override
    public int read() throws IOException {
        if (remaining == 0) return -1;
        int value = in.read();
        if (value != -1) remaining--;
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) return 0;
        if (remaining == 0) return -1;
        int count = in.read(bytes, offset, (int) Math.min(length, remaining));
        if (count > 0) remaining -= count;
        return count;
    }

    @Override
    public long skip(long count) throws IOException {
        long skipped = in.skip(Math.max(0, Math.min(count, remaining)));
        remaining -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(in.available(), remaining);
    }

    @Override public boolean markSupported() { return false; }
    @Override public void mark(int limit) {}
    @Override public void reset() throws IOException { throw new IOException("Mark is not supported."); }
}
