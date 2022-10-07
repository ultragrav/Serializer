package net.ultragrav.serializer;

import net.ultragrav.serializer.classes.Enums;
import net.ultragrav.serializer.classes.JsonMetaSerializables;
import net.ultragrav.serializer.util.RandomUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TestSerializers {
    private void testObject(Object obj) {
        GravSerializer ser = new GravSerializer();

        ser.writeObject(obj);

        Assertions.assertEquals(ser.readObject(), obj);
    }

    @Test
    public void testString() {
        String obj = RandomUtil.randomString(60);

        GravSerializer ser = new GravSerializer();

        ser.writeObject(obj);

        String s = ser.readObject();

        Assertions.assertEquals(s, obj);
    }

    public boolean strEquals(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        // Check equality manually.
        if (s1.length() != s2.length()) {
            return false;
        }
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                System.out.println("At index " + i + ", '" + s1.charAt(i) + "' != '" + s2.charAt(i) + "'");
                System.out.println("As numbers, " + (int) s1.charAt(i) + " != " + (int) s2.charAt(i));
                return false;
            }
        }
        return true;
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

    @Test
    public void testObjectArray() {
        String[][] test = {{"this", "is", "a", "test"}, {"this", "is", "also", "a", "test"}};

        GravSerializer ser = new GravSerializer();
        ser.writeObject(test);

        String[][] test2 = ser.readObject();
        Assertions.assertArrayEquals(test, test2);
    }

    @Test
    public void testBigInteger() {
        BigInteger test = new BigInteger("123456789101112131415161718192021222324252627282930313233343536373839404142434445464748495051525354555657585960616263646566676869707172737475767778798081828384858687888990919293949596979899100101102103104105106107108109110111112113114115116117118119120121122123124125126127128129130131132133134135136137138139140141142143144145146147148149150151152153154155156157158159160161162163164165166167168169170171172173174175176177178179180181182183184185186187188189190191192193194195196197198199200201202203204205206207208209210211212213214215216217218219220221222223224225226227228229230231232233234235236237238239240241242243244245246247248249250251252253254255256257258259260261262263264265266267268269270271272273274275276277278279280281282283284285286287288289290291292293294295296297298299300301302303304305306307308309310311312313314315316317318319320321322323324325326327328329330331332333334335336337338339340341342343344345346347348349350351352353354355356357358359360361362363364365366367368369370371372373374375376377378379380381382383384385386387388389390391392393394395396397398399400401402403404405406407408409410411412413414415416417418419420421422423424425426427428429430431432433434435436437438439440441442443444445446447448449450451452453454455456457458459460461462463464465466467468469470471472473474475476477478479480481482483484485486487488489490491492493494495496497498499500501502503504505506507508509510511512513514515516517518519520521522523524525526527528529530531532533");
        testObject(test);
    }

    @Test
    public void testBigDecimal() {
        BigDecimal test = new BigDecimal("123456789101112131415161718192021222324252627282930313233343536373839404142434445464748495051525354555657585960616263646566676869707172737475767778798081828384858687888990919293949596979899100101102103104105106107108109110111112113114115116117118119120121122123124125126127128129130131132133134135136137138139140141142143144145146147148149150151152153154155156157158159160161162163164165166167168169170171172173174175176177178179180181182183184185186187188189190191192193194195196197198199200201202203204205206207208209210211212213214215216217218219220221222223224225226227228229230231232233234235236237238239240241242243244245246247248249250251252253254255256257258259260261262263264265266267268269270271272273274275276277278279280281282283284285286287288289290291292293294295296297298299300301302303304305306307308309310311312313314315316317318319320321322323324325326327328329330331332333334335336337338339340341342343344345346347348349350351352353354355356357358359360361362363364365366367368369370371372373374375376377378379380381382383384385386387388389390391392393394395396397398399400401402403404405406407408409410411412413414415416417418419420421422423424425426427428429430431432433434435436437438439440441442443444445446447448449450451452453454455456457458459460461462463464465466467468469470471472473474475476477478479480481482483484485486487488489490491492493494495496497498499500501502503504505506507508509510511512513514515516517518519520521522523524525526527528529530531532533");
        testObject(test);
    }

    // TODO: Test remaining serializers
}
