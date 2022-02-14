package net.ultragrav.serializer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.ultragrav.serializer.classes.JsonMetaSerializables;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.ultragrav.serializer.util.RandomUtil.randomString;

public class TestJsonMetaSerializable {
    private JsonMetaSerializables.TestClassOne randomTestClass() {
        JsonMetaSerializables.TestClassOne ret = new JsonMetaSerializables.TestClassOne();
        ret.id = UUID.randomUUID();
        ret.name = randomString(7);
        ret.created = System.currentTimeMillis();
        ret.value = ThreadLocalRandom.current().nextInt(123456);
        return ret;
    }

    @Test
    public void testBasic() {
        JsonMetaSerializables.TestClassOne test = randomTestClass();
        JsonMeta meta = test.serialize();

        JsonMetaSerializables.TestClassOne res = new JsonMetaSerializables.TestClassOne(meta);
        assert Objects.equals(test, res);
    }

    @Test
    public void testSerialization() {
        JsonMetaSerializables.TestClassOne test = randomTestClass();
        GravSerializer ser = new GravSerializer();
        ser.writeObject(test);

        JsonMetaSerializables.TestClassOne res = ser.readObject();
        assert Objects.equals(test, res);
    }

    @Test
    public void testParameters() {
        UUID id = UUID.randomUUID();
        JsonMetaSerializables.TestClassParams test = new JsonMetaSerializables.TestClassParams(id, randomString(5));
        GravSerializer ser = new GravSerializer();
        ser.writeObject(test);

        JsonMetaSerializables.TestClassParams res = ser.readObject(id);

        assert Objects.equals(test, res);
    }

    @Test
    public void testParameters2() {
        UUID id = UUID.randomUUID();
        JsonMetaSerializables.TestClassParams test = new JsonMetaSerializables.TestClassParams(id, randomString(5));

        JsonMeta other = new JsonMeta();
        other.set("test", test);

        GravSerializer ser = new GravSerializer();
        ser.writeObject(other);

        JsonMeta newOther = ser.readObject();

        JsonMetaSerializables.TestClassParams res = newOther.getOrSet("test", null, id);

        assert Objects.equals(test, res);
    }
}
