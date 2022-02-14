package net.ultragrav.serializer.util;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {
    public static byte[] randomBytes(int len) {
        byte[] bt = new byte[len];
        ThreadLocalRandom.current().nextBytes(bt);
        return bt;
    }

    public static String randomString(int len) {
        return new String(randomBytes(len));
    }

    public static String randomStringAlpha(int len) {
        char[] valids = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        Random rand = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(valids[rand.nextInt(valids.length)]);
        }
        return builder.toString();
    }

    public static <T extends Enum<T>> T randomEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[ThreadLocalRandom.current().nextInt(clazz.getEnumConstants().length)];
    }
}
