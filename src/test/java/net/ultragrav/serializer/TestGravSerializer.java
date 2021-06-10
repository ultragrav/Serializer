package net.ultragrav.serializer;

import net.ultragrav.serializer.compressors.StandardCompressor;
import net.ultragrav.serializer.compressors.ZstdCompressor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGravSerializer {
    private byte[] createRandomBytes(int len) {
        byte[] bt = new byte[len];
        ThreadLocalRandom.current().nextBytes(bt);
        return bt;
    }

    @Test
    public void testBytes() {
        byte[] test = createRandomBytes(1000);
        GravSerializer ser = new GravSerializer();
        ser.writeByteArray(test);

        assert Arrays.equals(test, ser.readByteArray());
    }

    @Test
    public void testShorts() {
        Random rand = ThreadLocalRandom.current();
        GravSerializer ser = new GravSerializer();
        for (int i = 0; i < 100; i ++) {
            short sh = (short) rand.nextInt(0xFFFF);
            ser.writeShort(sh);

            assertEquals(sh, ser.readShort());
        }
    }

    @Test
    public void testIO() throws IOException {
        byte[] bt = createRandomBytes(10000);

        GravSerializer ser = new GravSerializer();
        ser.writeByteArray(bt);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ser.writeToStream(os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        GravSerializer ser2 = new GravSerializer(is);

        byte[] bt2 = ser2.readByteArray();

        assert Arrays.equals(bt, bt2);
    }

    @Test
    public void testCompression() throws IOException {
        byte[] bt = createRandomBytes(10000);

        GravSerializer ser = new GravSerializer();
        ser.writeByteArray(bt);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ser.writeToStream(os, StandardCompressor.instance);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        GravSerializer ser2 = new GravSerializer(is, StandardCompressor.instance);

        byte[] bt2 = ser2.readByteArray();

        assert Arrays.equals(bt, bt2);
    }

    @Test
    public void testCompressionZstd() throws IOException {
        byte[] bt = createRandomBytes(10000);

        GravSerializer ser = new GravSerializer();
        ser.writeByteArray(bt);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ser.writeToStream(os, ZstdCompressor.instance);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        GravSerializer ser2 = new GravSerializer(is, ZstdCompressor.instance);

        byte[] bt2 = ser2.readByteArray();

        assert Arrays.equals(bt, bt2);
    }
}
