package net.ultragrav.serializer.compressors;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import net.ultragrav.serializer.ArrayUtils;
import net.ultragrav.serializer.Compressor;
import net.ultragrav.serializer.DecompressionException;

public class ZstdCompressor implements Compressor {
    public static final ZstdCompressor instance = new ZstdCompressor();

    private ZstdCompressor() {
    }

    public synchronized byte[] compress(byte[] in) {
        int len = in.length;
        byte[] out = Zstd.compress(in);
        byte[] lenB = new byte[4];
        for (int i1 = 0; i1 < 4; i1++) //4 bytes in int
            lenB[i1] = (byte) (len >>> 8 * i1 & 255);
        return ArrayUtils.join(lenB, out);
    }

    public synchronized byte[] decompress(byte[] in) throws DecompressionException {
        if(in.length < 4) {
            return in;
        }
        int out = 0;
        for (int i = 0; i < 4; i++) {
            out |= ((int) in[i] << (i * 8)) & (255 << (i * 8));
        }
        byte[] dat = new byte[in.length - 4];
        System.arraycopy(in, 4, dat, 0, dat.length);
        try {
            return Zstd.decompress(dat, out);
        } catch(Exception e) {
            throw new DecompressionException(e.getMessage());
        }
    }
}
