package net.ultragrav.serializer.streams;

import net.ultragrav.serializer.GravSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GravSerializerOutputStream extends OutputStream {
    private GravSerializer serializer;

    public GravSerializerOutputStream(GravSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void write(int b) {
        serializer.writeByte((byte) b);
    }

    @Override
    public void write(byte[] b) {
        serializer.append(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        serializer.append(b, off, len);
    }

    public void writeObject(Object o) {
        serializer.writeObject(o);
    }
}
