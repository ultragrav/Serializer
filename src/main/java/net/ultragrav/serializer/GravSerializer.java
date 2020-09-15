package net.ultragrav.serializer;

import com.google.common.primitives.Bytes;
import net.ultragrav.serializer.compressors.StandardCompressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@SuppressWarnings("unchecked")
public class GravSerializer {
    private List<Byte> bytes = new ArrayList<>();
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
        while ((i = is.read()) != -1) {
            bytes.add((byte) i);
        }
        is.close();
        if (compressor != null) {
            byte[] data = toByteArray();
            byte[] output;
            try {
                output = compressor.decompress(data);
            } catch(DecompressionException e) {
                output = StandardCompressor.instance.decompress(data);
            }
            bytes = Bytes.asList(output);
        }
    }

    public GravSerializer(String input) {
        this(Base64.getDecoder().decode(input));
    }

    public GravSerializer(byte[] input) {
        bytes = Bytes.asList(input);
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
        writeMark = this.bytes.size();
    }

    /**
     * Resets serializer's writing position to the last writeMark
     */
    public void writeReset() {
        while(this.bytes.size() != writeMark) {
            this.bytes.remove(this.bytes.size()-1);
        }
    }

    /**
     * Resets to the last marker (mark()), by default it is 0
     */
    public void reset() {
        this.reading = mark;
    }

    public int size() {
        return this.bytes.size();
    }

    public void writeString(String str) {
        int size = str.length();
        writeInt(size);
        bytes.addAll(Bytes.asList(str.getBytes()));
    }

    public void writeByteArray(byte[] bites) {
        int size = bites.length;
        writeInt(size);
        this.bytes.addAll(Bytes.asList(bites));
    }

    public void writeShort(short value) {
        this.writeByte((byte) (value & 0xFF));
        this.writeByte((byte) (value & 0xFF00));
    }

    public void writeByte(byte bite) {
        this.bytes.add(bite);
    }

    /**
     * Returns the amount of remaining bytes
     */
    public int getRemaining() {
        return this.bytes.size() - this.reading;
    }

    public void writeLong(long l) {
        List<Byte> bites = new ArrayList<>();
        for (int i = 0; i < 8; i++) { //8 bytes in a long
            bites.add((byte) (l >>> 8 * i & 255)); //Isolate then add each byte BTW it's >>> because i don't want to preserve the sign, since I'm working with bytes, not their number representations unsure if i need it to be >>> but it's safer to have it
        }
        bytes.addAll(bites);
    }

    public void writeDouble(double d) {
        writeLong(Double.doubleToRawLongBits(d));
    }

    public void writeFloat(float d) {
        writeInt(Float.floatToIntBits(d));
    }

    public void writeInt(int i) {
        List<Byte> bites = new ArrayList<>();
        for (int i1 = 0; i1 < 4; i1++) //4 bytes in int
            bites.add((byte) (i >>> 8 * i1 & 255)); //Isolate then add each byte
        bytes.addAll(bites);
    }

    public void writeBoolean(boolean bool) {
        this.writeByte((byte) (bool ? 1 : 0));
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
        long out = 0L;
        for (int i = 0; i < 8; i++) {
            out |= ((long) readByte() << (i * 8)) & ((long) 255 << (i * 8));
        }
        return out;
    }

    public short readShort() {
        byte b1 = this.readByte();
        byte b2 = this.readByte();
        return (short) (b2 << 8 | b1);
    }

    public boolean readBoolean() {
        return this.readByte() == 1;
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public byte[] readByteArray() {
        int length = readInt();
        byte[] bites = new byte[length];
        for (int i = 0; i < length; i++) {
            bites[i] = readByte();
        }
        return bites;
    }

    public byte readByte() {
        if (reading >= bytes.size())
            throw new IllegalStateException("End of byte array reached (GravSerializer)");
        return bytes.get(reading++);
    }

    public boolean hasNext() {
        return reading < bytes.size();
    }

    public void append(GravSerializer serializer) {
        this.bytes.addAll(serializer.bytes);
    }

    public void writeUUID(UUID id) {
        this.writeLong(id.getMostSignificantBits());
        this.writeLong(id.getLeastSignificantBits());
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public int readInt() {
        int out = 0;
        for (int i = 0; i < 4; i++) {
            out |= ((int) readByte() << (i * 8)) & (255 << (i * 8));
        }
        return out;
    }

    public String readString() {
        int size = readInt();
        byte[] bites = new byte[size];
        for (int i = 0; i < size; i++) {
            bites[i] = readByte();
        }
        return new String(bites);
    }

    public String toString() {
        byte[] bites = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            bites[i] = bytes.get(i);
        }
        return Base64.getEncoder().encodeToString(bites);
    }

    public byte[] toByteArray() {
        return Bytes.toArray(bytes);
    }

    public void writeToStream(OutputStream stream) throws IOException {
        writeToStream(stream, null);
    }

    public void writeToStream(OutputStream stream, Compressor compressor) throws IOException {
        byte[] bt = toByteArray();

        if (compressor != null) {
            bt = compressor.compress(bt);
        }

        for (byte b : bt) {
            stream.write(b);
        }
        stream.flush();
        stream.close();
    }
}
