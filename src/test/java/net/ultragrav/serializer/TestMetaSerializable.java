package net.ultragrav.serializer;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TestMetaSerializable {
    private String randomString(int len) {
        char[] valids = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        Random rand = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            builder.append(valids[rand.nextInt(valids.length)]);
        }
        return builder.toString();
    }

    private TestClassOne randomTestClass() {
        TestClassOne ret = new TestClassOne();
        ret.id = UUID.randomUUID();
        ret.name = randomString(7);
        ret.created = System.currentTimeMillis();
        ret.value = ThreadLocalRandom.current().nextInt(123456);
        return ret;
    }

    @Test
    public void testBasic() {
        TestClassOne test = randomTestClass();
        Meta meta = test.serialize();

        TestClassOne res = new TestClassOne(meta);
        assert Objects.equals(test, res);
    }

    @Test
    public void testSerialization() {
        TestClassOne test = randomTestClass();
        GravSerializer ser = new GravSerializer();
        ser.writeObject(test);

        TestClassOne res = ser.readObject();
        assert Objects.equals(test, res);
    }

    public static class TestClassOne implements MetaSerializable {
        private UUID id;
        private String name;
        private long created;
        private int value;

        public TestClassOne() {
        }

        public TestClassOne(Meta meta) {
            this.id = meta.get("id");
            this.name = meta.get("name");
            this.created = meta.get("created");
            this.value = meta.get("value");
        }

        @Override
        public Meta serialize() {
            Meta ret = new Meta();
            ret.set("id", id);
            ret.set("name", name);
            ret.set("created", created);
            ret.set("value", value);
            return ret;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClassOne that = (TestClassOne) o;
            return created == that.created &&
                    value == that.value &&
                    Objects.equals(id, that.id) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, created, value);
        }
    }
}
