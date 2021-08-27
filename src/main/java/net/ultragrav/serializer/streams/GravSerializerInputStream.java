package net.ultragrav.serializer.streams;

import net.ultragrav.serializer.GravSerializer;

import java.io.IOException;
import java.io.InputStream;

public class GravSerializerInputStream extends InputStream {
    private GravSerializer serializer;

    public GravSerializerInputStream(GravSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public int read() {
        if (serializer.hasNext()) return serializer.readByte();
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        return serializer.readBytes(b, off, len);
    }

    public <T> T readObject(Object... args) {
        return serializer.readObject(args);
    }

    @Override
    public long skip(long n) {
        return serializer.skip(n);
    }

    @Override
    public int available() throws IOException {
        return serializer.getRemaining();
    }

    /**
     * Mark the serializer, note:
     * readlimit is not supported
     * @param readlimit Unsupported
     */
    @Override
    public synchronized void mark(int readlimit) {
        serializer.mark();
    }

    /**
     * Reset to the marked position
     * If no position has been marked the stream
     * will reset to the 0 position
     */
    @Override
    public synchronized void reset() {
        serializer.reset();
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}
