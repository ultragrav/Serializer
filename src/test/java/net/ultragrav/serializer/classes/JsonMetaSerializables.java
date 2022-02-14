package net.ultragrav.serializer.classes;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.ultragrav.serializer.JsonMeta;
import net.ultragrav.serializer.JsonMetaSerializable;

import java.util.UUID;

public class JsonMetaSerializables {
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class TestClassOne implements JsonMetaSerializable {
        public UUID id;
        public String name;
        public long created;
        public int value;

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
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TestClassParams implements JsonMetaSerializable {
        public UUID id;
        public String name;

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
    }
}
