package net.ultragrav.serializer;

public interface Compressor {
    byte[] compress(byte[] in);
    byte[] decompress(byte[] in) throws DecompressionException;
}
