package net.ultragrav.serializer;

import net.ultragrav.serializer.compressors.StandardCompressor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class GravSerializer implements GravSerializable {
    private byte[] bytes = new byte[16]; //Default 16 byte capacity
    private int used = 0;
    private int reading = 0;
    private int mark = 0;
    private int writeMark = 0;

    public GravSerializer() {
    }

    public GravSerializer(InputStream is) throws IOException {
        this(is, null);
    }

    public GravSerializer(InputStream is, Compressor compressor) throws IOException {
        int i;
        ensureCapacity(is.available());
        byte[] buff = new byte[2048];
        while ((i = is.read(buff)) != -1) {
            append(buff, i);
        }
        is.close();
        if (compressor != null) {
            byte[] data = toByteArray();
            byte[] output;
            try {
                output = compressor.decompress(data);
            } catch (DecompressionException e) {
                output = StandardCompressor.instance.decompress(data);
            }
            bytes = output;
            used = bytes.length;
        }
    }

    public GravSerializer(String input) {
        this(Base64.getDecoder().decode(input));
    }

    public GravSerializer(byte[] input) {
        this(input, true);
    }

    /**
     * Create a serializer optionally not copying the array. This might be
     * faster in exchange that the input array might be modified. I recommend
     * only using this only when you're deserializing.
     */
    public GravSerializer(byte[] input, boolean copyArray) {
        if (copyArray) {
            bytes = Arrays.copyOf(input, input.length);
        } else {
            bytes = input;
        }
        used = bytes.length;
    }

    /**
     * Sets the marker to the current reading byte
     * When you do reset() it will reset to the current reading byte
     */
    public void mark() {
        mark = reading;
    }

    /**
     * Marks this write position for a later writeReset
     */
    public void writeMark() {
        writeMark = used;
    }

    /**
     * Resets serializer's writing position to the last writeMark and sets the writeMark to the current write position.
     */
    public void writeReset() {
        int temp = used;
        used = writeMark;
        writeMark = temp;
    }

    public long skip(long n) {
        if (reading + n > used) {
            int skipped = used - reading;
            reading = used;
            return skipped;
        } else {
            reading += n;
            return n;
        }
    }

    public void append(byte[] arr) {
        append(arr, arr.length);
    }

    public void append(byte[] arr, int len) {
        append(arr, 0, len);
    }
    public void append(byte[] arr, int off, int len) {
        if (len > arr.length)
            throw new IllegalArgumentException("Size cannot be larger than array size!");

        ensureCapacity(used + len);
        System.arraycopy(arr, off, this.bytes, this.used, len);
        this.used += len;
    }

    public void ensureCapacity(int capacity) {
        if (this.bytes.length >= capacity)
            return;

        int oldCapacity = this.bytes.length;
        int newCapacity = Math.max(oldCapacity + (oldCapacity >> 1), capacity); // max(old + (old/2), cap)
        byte[] b = new byte[newCapacity];
        System.arraycopy(this.bytes, 0, b, 0, this.used);
        this.bytes = b;
    }

    /**
     * Resets to the last marker (mark()) and sets the mark to the current read position.
     */
    public void reset() {
        int temp = reading;
        reading = mark;
        mark = temp;
    }

    public int size() {
        return this.used;
    }

    public int trueSize() {
        return this.bytes.length;
    }

    public void writeString(String str) {
        if (str == null) {
            writeInt(-1);
            return;
        }
        writeByteArray(str.getBytes());
    }

    public String readString() {
        int size = readInt();
        if (size == -1) return null;
        byte[] bites = readBytes(size);
        return new String(bites);
    }

    public void writeByteArray(byte[] bites) {
        int size = bites.length;
        ensureCapacity(used + size + 4);
        writeInt(size);
        append(bites);
    }

    public void writeShort(short value) {
        this.writeByte((byte) (value & 0xFF));
        this.writeByte((byte) (value >>> 8));
    }

    public void writeByte(byte bite) {
        ensureCapacity(used + 1);
        this.bytes[used++] = bite;
    }

    /**
     * Returns the amount of remaining bytes
     */
    public int getRemaining() {
        return this.used - this.reading;
    }

    public void writeLong(long l) {
        writeLong(l, false);
    }

    public void writeLong(long l, boolean littleEndian) {
        byte[] bites = new byte[8];
        if (!littleEndian) {
            for (int i = 0; i < 8; i++) { //8 bytes in a long
                bites[i] = ((byte) (l >>> 8 * i & 255)); //Isolate then add each byte BTW it's >>> because i don't want to preserve the sign, since I'm working with bytes, not their number representations unsure if i need it to be >>> but it's safer to have it
            }
        } else {
            for (int i = 0; i < 8; i++) { //8 bytes in a long
                bites[7 - i] = ((byte) (l >>> 8 * i & 255)); //Isolate then add each byte BTW it's >>> because i don't want to preserve the sign, since I'm working with bytes, not their number representations unsure if i need it to be >>> but it's safer to have it
            }
        }
        append(bites);
    }

    public void writeDouble(double d) {
        writeDouble(d, false);
    }

    public void writeDouble(double d, boolean littleEndian) {
        writeLong(Double.doubleToRawLongBits(d), littleEndian);
    }

    public void writeFloat(float d) {
        writeFloat(d, false);
    }

    public void writeFloat(float d, boolean littleEndian) {
        writeInt(Float.floatToIntBits(d), littleEndian);
    }

    public void writeInt(int i) {
        writeInt(i, false);
    }

    public void writeInt(int i, boolean littleEndian) {
        byte[] bites = new byte[4];

        if (!littleEndian) {
            for (int i1 = 0; i1 < 4; i1++) //4 bytes in int
                bites[i1] = ((byte) (i >>> 8 * i1 & 255)); //Isolate then add each byte
        } else {
            for (int i1 = 0; i1 < 4; i1++) //4 bytes in int
                bites[3 - i1] = ((byte) (i >>> 8 * i1 & 255)); //Isolate then add each byte
        }
        append(bites);
    }

    public void writeVarInt(long l) {
        while (l != 0) {
            boolean sig = l >>> 7 != 0;
            byte b = (byte) ((sig ? 0b10000000 : 0) | (l & 0b01111111));
            writeByte(b);
            l >>>= 7;
        }
    }

    public long readVarInt() {
        long l = 0;
        int c = 0;
        byte b = (byte) 0xFF;
        while ((b & 0b10000000) != 0) {
            b = readByte();
            l |= (b & 0b01111111L) << 7 * c;
            c++;
        }
        return l;
    }

    public void writeChar(char ch) {
        writeVarInt((int) ch);
    }

    public char readChar() {
        return (char) (int) readVarInt();
    }

    public void writeBigInteger(BigInteger i) {
        writeByteArray(i.toByteArray());
    }

    public BigInteger readBigInteger() {
        return new BigInteger(readByteArray());
    }

    public void writeBoolean(boolean bool) {
        this.writeByte((byte) (bool ? 1 : 0));
    }

    public void writeBooleanArray(boolean... bools) {
        int len = bools.length >> 3;
        byte rem = (byte) (bools.length % 8);

        byte[] ret = new byte[len + (rem > 0 ? 1 : 0)];

        for (int i = 0; i < len; i++) {
            byte b = 0;
            for (byte j = 0; j < 8; j++) {
                if (bools[i << 3 | j]) {
                    b |= 1;
                }
                if (j == 7)
                    break;
                b <<= 1;
            }
            ret[i] = b;
        }
        if (rem > 0) {
            byte b = 0;
            for (byte j = 0; j < rem; j++) {
                if (bools[len << 3 | j]) {
                    b |= 1;
                }
                b <<= 1;
            }
            ret[len] = b;
        }

        writeByte(rem);
        writeByteArray(ret);
    }

    public boolean[] readBooleanArray() {
        byte rem = readByte();
        byte[] dat = readByteArray();
        int len = dat.length - (rem > 0 ? 1 : 0);

        boolean[] ret = new boolean[len << 3 | rem];

        for (int i = 0; i < len; i++) {
            byte b = dat[i];
            for (int j = 0; j < 8; j++) {
                ret[i << 3 | j] = ((b >>> 7) & 1) == 1;
                b <<= 1;
            }
        }
        if (rem > 0) {
            byte b = dat[len];
            b <<= (7 - rem);
            for (int j = 0; j < rem; j++) {
                ret[len << 3 | j] = ((b >>> 7) & 1) == 1;
                b <<= 1;
            }
        }

        return ret;
    }

    public void writeObject(Object o) {
        Serializers.serializeObject(this, o);
    }

    public <T> T readObject(Object... args) {
        return (T) Serializers.deserializeObject(this, args);
    }

    public GravSerializer readSerializer() {
        return new GravSerializer(this.readByteArray());
    }

    public void writeSerializer(GravSerializer serializer) {
        this.writeByteArray(serializer.toByteArray());
    }

    public long readLong() {
        return readLong(false);
    }

    public long readLong(boolean littleEndian) {
        long out = 0L;
        if (!littleEndian) {
            for (int i = 0; i < 8; i++) {
                out |= ((long) readByte() << (i * 8)) & ((long) 255 << (i * 8));
            }
        } else {
            for (int i = 7; i >= 0; i--) {
                out |= ((long) readByte() << (i * 8)) & ((long) 255 << (i * 8));
            }
        }
        return out;
    }

    public short readShort() {
        byte b1 = this.readByte();
        byte b2 = this.readByte();
        return (short) (b2 << 8 | (b1 & 0xFF));
    }

    public boolean readBoolean() {
        return this.readByte() == 1;
    }

    public double readDouble() {
        return readDouble(false);
    }

    public double readDouble(boolean littleEndian) {
        return Double.longBitsToDouble(readLong(littleEndian));
    }

    public float readFloat() {
        return readFloat(false);
    }

    public float readFloat(boolean littleEndian) {
        return Float.intBitsToFloat(readInt(littleEndian));
    }

    public byte[] readByteArray() {
        int length = readInt();
        return readBytes(length);
    }

    public byte readByte() {
        if (reading >= used)
            throw new IllegalStateException("End of byte array reached (GravSerializer)");
        return bytes[reading++];
    }

    public byte[] readBytes(int len) {
        byte[] ret = new byte[len];
        int i;
        if ((i = readBytes(ret)) != len) {
            reading -= i;
            throw new IllegalStateException("Insufficient bytes to read");
        }
        return ret;
    }
    public int readBytes(byte[] buf) {
        return readBytes(buf, 0, buf.length);
    }
    public int readBytes(byte[] buf, int off, int len) {
        int count = Math.min(len, used - reading);
        System.arraycopy(bytes, reading, buf, off, count);
        reading += count;
        return count;
    }

    public boolean hasNext() {
        return reading < used;
    }

    public void append(GravSerializer serializer) {
        append(serializer.bytes, serializer.used);
    }

    public void writeUUID(UUID id) {
        ensureCapacity(used + 16);
        this.writeLong(id.getMostSignificantBits());
        this.writeLong(id.getLeastSignificantBits());
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public int readInt() {
        return readInt(false);
    }

    public int readInt(boolean littleEndian) {
        int out = 0;

        if (!littleEndian) {
            for (int i = 0; i < 4; i++) {
                out |= ((int) readByte() << (i * 8)) & (255 << (i * 8));
            }
        } else {
            for (int i = 3; i >= 0; i--) {
                out |= ((int) readByte() << (i * 8)) & (255 << (i * 8));
            }
        }
        return out;
    }

    public String toString() {
        return Base64.getEncoder().encodeToString(toByteArray());
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(bytes, used);
    }

    public void writeToStream(OutputStream stream) throws IOException {
        writeToStream(stream, null);
    }

    public void writeToStream(OutputStream stream, Compressor compressor) throws IOException {
        byte[] bt = toByteArray();

        if (compressor != null) {
            bt = compressor.compress(bt);
        }

        stream.write(bt);
        stream.flush();
        stream.close();
    }

    public static void main(String[] args) {
        GravSerializer ser = new GravSerializer();
        ser.writeFloat(0.05f, true);
        ser.writeFloat(0.1f, true);
        System.out.println(Arrays.toString(ser.toByteArray()));
    }

    public static GravSerializer deserialize(GravSerializer serializer) {
        int length = serializer.readInt();
        byte[] arr = serializer.readBytes(length);
        return new GravSerializer(arr);
    }

    @Override
    public void serialize(GravSerializer serializer) {
        serializer.writeInt(used);
        serializer.append(bytes, used);
    }
}
