package net.ultragrav.serializer;

import net.ultragrav.serializer.compressors.StandardCompressor;
import net.ultragrav.serializer.compressors.ZstdCompressor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
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
    public void testBooleanArrays() {
        Random rand = new Random();
        boolean[] bools = new boolean[1000000];
        for (int i = 0; i < bools.length; i ++) {
            bools[i] = rand.nextBoolean();
        }

        GravSerializer ser = new GravSerializer();
        ser.writeBooleanArray(bools);

        boolean[] res = ser.readBooleanArray();
        assert Arrays.equals(bools, res);
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

    @Test
    public void testVarInt() {
        GravSerializer ser = new GravSerializer();

        long num = (long) (Math.random() * Long.MAX_VALUE);
        ser.writeVarInt(num);

        long var = ser.readVarInt();

        assert var == num;
    }

    @Test
    public void testBigInteger() {
        Random random = new Random();

        byte[] bival = new byte[random.nextInt(12) + 12];
        random.nextBytes(bival);
        BigInteger i = new BigInteger(bival);

        GravSerializer ser = new GravSerializer();
        ser.writeBigInteger(i);

        BigInteger i2 = ser.readBigInteger();

        assert i.equals(i2);
    }
}
