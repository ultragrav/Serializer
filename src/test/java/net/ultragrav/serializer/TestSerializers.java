package net.ultragrav.serializer;

import net.ultragrav.serializer.classes.Enums;
import net.ultragrav.serializer.classes.JsonMetaSerializables;
import net.ultragrav.serializer.util.RandomUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSerializers {
    private void testObject(Object obj) {
        GravSerializer ser = new GravSerializer();

        ser.writeObject(obj);

        Assertions.assertEquals(ser.readObject(), obj);
    }

    @Test
    public void testString() {
        String obj = RandomUtil.randomString(50);

        GravSerializer ser = new GravSerializer();

        ser.writeObject(obj);

        Assertions.assertEquals(ser.readObject(), obj);
    }

    @Test
    public void testByteArray() {
        byte[] obj = RandomUtil.randomBytes(50);

        GravSerializer ser = new GravSerializer();

        ser.writeObject(obj);

        Assertions.assertArrayEquals(ser.readObject(), obj);
    }

    @Test
    public void testByte() {
        testObject((byte) 128);
    }

    @Test
    public void testDouble() {
        testObject(12378.2347D);
    }

    @Test
    public void testInteger() {
        testObject(123456789);
    }

    @Test
    public void testLong() {
        testObject(1234567891011121314L);
    }

    @Test
    public void testUUID() {
        testObject(UUID.randomUUID());
    }

//    @Test
//    public void testMap() {
//        Map<Object, Object> testMap = new HashMap<>();
//
//        testMap.put(UUID.randomUUID(), RandomUtil.randomStringAlpha(10));
//        testMap.put(RandomUtil.randomBytes(5), RandomUtil.randomString(20));
//
//        testObject(testMap);
//    }

//    @Test
//    public void testList() {
//        List<Object> testList = new ArrayList<>();
//
//        testList.add(UUID.randomUUID());
//        testList.add(RandomUtil.randomBytes(5));
//        testList.add(RandomUtil.randomBytes(15));
//
//        testObject(testList);
//    }

    @Test
    public void testEnum() {
        testObject(RandomUtil.randomEnum(Enums.NoAnnotation.class));
        testObject(RandomUtil.randomEnum(Enums.AnnotationOrdinal.class));
        testObject(RandomUtil.randomEnum(Enums.AnnotationName.class));
    }

    @Test
    public void testSerializable() {
        testObject(new JsonMetaSerializables.TestClassOne(
                UUID.randomUUID(), RandomUtil.randomString(20),
                ThreadLocalRandom.current().nextLong(Long.MAX_VALUE),
                ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
    }

    // TODO: Test remaining serializers
}
