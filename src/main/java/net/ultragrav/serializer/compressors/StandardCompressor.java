package net.ultragrav.serializer.compressors;

import net.ultragrav.serializer.Compressor;
import net.ultragrav.serializer.DecompressionException;

import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class StandardCompressor implements Compressor {
    public static final StandardCompressor instance = new StandardCompressor();

    private StandardCompressor() {
    }

    public byte[] compress(byte[] in) {
        Deflater deflater = new Deflater();
        deflater.setInput(in);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            os.write(buffer, 0, count);
        }
        return os.toByteArray();
    }

    public byte[] decompress(byte[] in) throws DecompressionException {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(in);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(in.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            return outputStream.toByteArray();
        } catch (IOException | DataFormatException e) {
            throw new DecompressionException(e.getMessage());
        }
    }
}
