package net.ultragrav.serializer.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {
    public static String randomString(int len) {
        char[] valids = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        Random rand = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(valids[rand.nextInt(valids.length)]);
        }
        return builder.toString();
    }
}
