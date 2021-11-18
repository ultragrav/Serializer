package net.ultragrav.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.ultragrav.serializer.util.RandomUtil.randomString;

public class TestJsonMetaSerializable {
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
        JsonMeta meta = test.serialize();

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

    @Test
    public void testParameters() {
        UUID id = UUID.randomUUID();
        TestClassParams test = new TestClassParams(id, randomString(5));
        GravSerializer ser = new GravSerializer();
        ser.writeObject(test);

        TestClassParams res = ser.readObject(id);

        assert Objects.equals(test, res);
    }

    @Test
    public void testParameters2() {
        UUID id = UUID.randomUUID();
        TestClassParams test = new TestClassParams(id, randomString(5));

        JsonMeta other = new JsonMeta();
        other.set("test", test);

        GravSerializer ser = new GravSerializer();
        ser.writeObject(other);

        JsonMeta newOther = ser.readObject();

        TestClassParams res = newOther.getOrSet("test", null, id);

        assert Objects.equals(test, res);
    }

    public static class TestClassOne implements JsonMetaSerializable {
        private UUID id;
        private String name;
        private long created;
        private int value;

        public TestClassOne() {
        }

        public TestClassOne(JsonMeta meta) {
            this.id = meta.get("id");
            this.name = meta.get("name");
            this.created = meta.get("created");
            this.value = meta.get("value");
        }

        @Override
        public JsonMeta serialize() {
            JsonMeta ret = new JsonMeta();
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

    @Getter
    @AllArgsConstructor
    public static class TestClassParams implements JsonMetaSerializable {
        private UUID id;
        private String name;

        public TestClassParams(JsonMeta meta, UUID id) {
            this.id = id;

            this.name = meta.get("name");
        }

        @Override
        public JsonMeta serialize() {
            JsonMeta meta = new JsonMeta();
            meta.set("name", name);
            return meta;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestClassParams that = (TestClassParams) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            return name != null ? name.equals(that.name) : that.name == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }
}
