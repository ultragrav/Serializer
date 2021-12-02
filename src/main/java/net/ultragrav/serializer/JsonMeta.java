package net.ultragrav.serializer;

import net.ultragrav.serializer.util.JsonUtil;

import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

//TODO make thread-safe
public class JsonMeta implements GravSerializable {
    private static final int FORMAT_VERSION = 2;
    private static final String SERIALIZED_PREFIX = "__$$";

    private final Map<String, Object> data = new HashMap<>();
    public final Map<String, GravSerializer> toDeserialize = new HashMap<>();

    private String delimiter = "\\.";
    private String[] path = new String[0];
    private JsonMeta parent = null;

    private volatile ReentrantLock lock = new ReentrantLock();

    private boolean markDirtyByDefault = false;

    private final JsonMetaUpdateRecord record = new JsonMetaUpdateRecord();

    /**
     * markDirtyByDefault defaults to false. You must set markDirtyByDefault to true if you
     * want this JsonMeta object to automatically keep records (Mark dirty).
     */
    public JsonMeta() {
    }

    public JsonMeta(boolean useRecord) {
        this.markDirtyByDefault = useRecord;
    }

    @SuppressWarnings("unchecked")
    public JsonMeta(Map<String, Object> map) {
        for (Map.Entry<String, Object> ent : map.entrySet()) {
            set(ent.getKey(), ent.getValue(), false);
        }
    }

    public JsonMeta(String delimiter) {
        this.delimiter = delimiter;
    }

    public boolean isMarkDirtyByDefault() {
        return markDirtyByDefault;
    }

    public void setMarkDirtyByDefaultRecursive(boolean value) {
        this.markDirtyByDefault = value;
        lock.lock();
        for (Object o : data.values()) {
            if (o instanceof JsonMeta) {
                JsonMeta m = (JsonMeta) o;
                if (m.parent != this)
                    continue;
                m.setMarkDirtyByDefaultRecursive(value);
            }
        }
        lock.unlock();
    }

    public JsonMetaUpdateRecord getRecord() {
        return record;
    }

    public List<String> getKeys() {
        lock.lock();
        try {
            return new ArrayList<>(this.data.keySet());
        } finally {
            lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public <T> T get(String path, Object... constructionArgs) {
        return get(path.split(delimiter), constructionArgs);
    }

    public <T> T get(String[] path, Object... constructionArgs) {
        lock.lock();
        try {
            JsonMeta current = this;
            for (int i = 0, pathLength = path.length; i < pathLength; i++) {
                String s = path[i];
                Object o = current.data.get(s);
                if (o instanceof JsonMeta) {
                    current = (JsonMeta) o;
                } else {
                    if (i != path.length - 1)
                        return null;
                    if (o == null) {
                        GravSerializer ser = current.toDeserialize.get(s);
                        if (ser != null) {
                            o = ser.readObject(constructionArgs);
                            if (o != null) {
                                current.data.put(s, o);
                                current.toDeserialize.remove(s);
                            }
                        }
                    }
                    return (T) o;
                }
            }
            if (current instanceof JsonMeta) {
                try {
                    return (T) current;
                } catch (ClassCastException e) {
                    return (T) current.asMap();
                }
            }
            return (T) current;
        } finally {
            lock.unlock();
        }
    }

    public boolean has(String path) {
        return get(path) != null;
    }

    public <T> T getOrSet(String path, T defaultValue, Object... constructionArgs) {
        return getOrSet(path, defaultValue, markDirtyByDefault, constructionArgs);
    }

    public <T> T getOrSet(String path, T defaultValue, boolean markDirty, Object... constructionArgs) {
        lock.lock();
        try {
            T obj = get(path, constructionArgs);
            if (obj == null) {
                set(path, defaultValue, markDirty);
                return defaultValue;
            }
            return obj;
        } finally {
            lock.unlock();
        }
    }

    public void remove(String path) {
        set(path, null);
    }

    public void remove(String path, boolean markDirty) {
        set(path, null, markDirty);
    }

    public JsonMeta set(String path, Object value) {
        return set(path.split(delimiter), value);
    }

    public JsonMeta set(String path, Object value, boolean markDirty) {
        return set(path.split(delimiter), value, markDirty);
    }

    public JsonMeta set(String[] path, Object value) {
        return set(path, value, markDirtyByDefault);
    }

    /**
     * Set a key to a value. Using markDirty=false will be faster.
     */
    public JsonMeta set(String[] path, Object value, boolean markDirty) { //Doesn't use recursion, may change later
        lock.lock();
        try {
            JsonMeta current = this;
            for (int i = 0, pathLength = path.length; i < pathLength; i++) {
                String s = path[i];
                Object o = current.data.get(s);

                if (i == pathLength - 1) {

                    Object prev;
                    if (value == null) {
                        prev = current.data.remove(s);
                    } else {
                        prev = current.data.put(s, value);
                    }
                    if (markDirty) {
                        current.markDirty(s);
                    }

                    if (value instanceof Map) {
                        Map<?, ?> vMap = (Map<?, ?>) value;
                        if (vMap.size() == 0) {
                            value = new JsonMeta();
                        } else {
                            JsonMeta submeta = new JsonMeta();
                            for (Map.Entry<?, ?> ent : vMap.entrySet()) {
                                if (ent.getKey() instanceof String) {
                                    submeta.set((String) ent.getKey(), ent.getValue());
                                } else {
                                    submeta = null;
                                    break;
                                }
                            }
                            if (submeta != null)
                                value = submeta;
                        }
                    }
                    if (value instanceof Meta) {
                        value = ((Meta) value).toJsonMeta();
                    }

                    //Link it if it's a JsonMeta
                    if (value instanceof JsonMeta) {
                        JsonMeta v = (JsonMeta) value;
                        link(current, v, s);

                        if (markDirty) {
                            v.getRecord().clear();
                            if (prev instanceof JsonMeta) {
                                v.markDirtyDiff((JsonMeta) prev);
                            } else {
                                v.markDirtyRecursive();
                            }
                        }
                    }
                } else if (!(o instanceof JsonMeta)) {
                    //Create new json meta at specific path
                    JsonMeta next = new JsonMeta(delimiter);

                    link(current, next, path[i]);

                    //Put the next JsonMeta
                    current.data.put(s, next);
                    current = next;
                } else {
                    current = (JsonMeta) o;
                }
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    public void putAll(JsonMeta meta) {
        putAll(meta, true);
    }

    /**
     * Take everything from the supplied meta and merge it into this meta, replacing identical keys
     */
    public void putAll(JsonMeta meta, boolean markDirty) {
        lock.lock();
        meta.lock.lock();

        try {
            for (String key : meta.getKeys()) {
                Object next = meta.get(new String[]{key});
                Object current = this.get(new String[]{key});

                if (next instanceof JsonMeta && current instanceof JsonMeta) {
                    ((JsonMeta) current).putAll((JsonMeta) next);
                } else {
                    set(new String[]{key}, next, markDirty);
                }
            }
        } finally {
            meta.lock.unlock();
            lock.unlock();
        }
    }

    /**
     * @param valName The last entry in the path that the child should inherit eg. adding meta A to B at apple.dog the last entry would be dog
     */
    private static void link(JsonMeta parent, JsonMeta child, String valName) {

        //Compute current path
        String[] currentPath = Arrays.copyOf(parent.path, parent.path.length + 1);
        currentPath[currentPath.length - 1] = valName;

        // Must lock with child's lock, because if the child's lock is already held while we set the child's lock to a new one,
        // then the holder won't be able to unlock the previous lock
        ReentrantLock childLock = child.lock;
        childLock.lock();

        //Set lock
        child.lock = parent.lock;
        child.markDirtyByDefault = parent.markDirtyByDefault;

        //Switch to the new lock
        childLock.unlock();
        parent.lock.lock();

        //Set paths
        child.path = currentPath;
        child.parent = parent;

        //Set record's children
        parent.record.children.add(child.record);

        //Unlock
        parent.lock.unlock();

    }

    private void markDirty(String key) {
        record.markDirty(key);
        if (parent != null) {
            if (path.length == 0) throw new IllegalStateException("This shouldn't happen.");
            parent.markDirty(path[path.length - 1]);
        }
    }

    private void markDirtyRecursive() {
        for (Map.Entry<String, Object> ent : data.entrySet()) {
            record.markDirty(ent.getKey());
            if (ent.getValue() instanceof JsonMeta) {
                ((JsonMeta) ent.getValue()).markDirtyRecursive();
            }
        }
        if (parent != null) {
            if (path.length == 0) throw new IllegalStateException("This shouldn't happen.");
            parent.markDirty(path[path.length - 1]);
        }
    }

    private void markDirtyDiff(JsonMeta other) {
        boolean changed = false;
        for (Map.Entry<String, Object> ent : data.entrySet()) {
            if (!other.data.containsKey(ent.getKey())) {
                record.markDirty(ent.getKey());
                changed = true;
                continue;
            }

            if (ent.getValue() instanceof JsonMeta) {
                if (!(other.data.get(ent.getKey()) instanceof JsonMeta)) {
                    record.markDirty(ent.getKey());
                    changed = true;
                } else {
                    ((JsonMeta) ent.getValue()).markDirtyDiff((JsonMeta) other.data.get(ent.getKey()));
                }
            } else if (!Objects.equals(ent.getValue(), other.data.get(ent.getKey()))) {
                record.markDirty(ent.getKey());
                changed = true;
            }
        }
        if (parent != null && changed) {
            if (path.length == 0) throw new IllegalStateException("This shouldn't happen.");
            parent.markDirty(path[path.length - 1]);
        }
    }

    public JsonMeta getRoot() {
        return parent == null ? this : parent.getRoot();
    }

    /**
     * Literally just get, but casted to JsonMeta immediately
     */
    public JsonMeta getAsJsonMeta(String path) {
        return get(path);
    }

    @Override
    public String toString() {
        return recursiveToString(0);
    }
    public String toStringFull() {
        return recursiveFullToString(0);
    }

    private String recursiveFullToString(int indentation) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentation; i++) {
            indent.append("  ");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{").append("\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object val = entry.getValue();
            String valStr = val == null ? "null" :
                    val instanceof JsonMeta ? ((JsonMeta) val).recursiveToString(indentation + 1) : val.toString();
            builder.append(indent).append("  ").append(entry.getKey()).append(": ").append(valStr).append("\n");
        }
        builder.append(indent).append("}");
        if (!toDeserialize.isEmpty()) {
            builder.append("\n").append(indent).append("toDeserialize: {\n");
            for (Map.Entry<String, GravSerializer> toD : toDeserialize.entrySet()) {
                builder.append(indent).append("  ").append(toD.getKey()).append(": ").append(new String(toD.getValue().toByteArray())).append("\n");
            }
            builder.append(indent).append("}");
        }

        return builder.toString();
    }

    private String recursiveToString(int indentation) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentation; i++) {
            indent.append("  ");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{").append("\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object val = entry.getValue();
            String valStr = val == null ? "null" :
                    val instanceof JsonMeta ? ((JsonMeta) val).recursiveToString(indentation + 1) : val.toString();
            builder.append(indent).append("  ").append(entry.getKey()).append(": ").append(valStr).append("\n");
        }

        return builder.append(indent).append("}").toString();
    }

    /**
     * Convert an object to json, maybe works!
     *
     * @return JSON representation of this JsonMeta
     */
    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        String objs = data.entrySet().stream()
                .map(ent -> "\"" + ent.getKey() + "\": " + jsonStringify(ent.getValue()))
                .collect(Collectors.joining(",\n"));
        builder.append(objs);
        return builder.append("\n}").toString();
    }

    /**
     * Convert this JsonMeta to a JsonMeta with only JSON-supported types (serialize all other objects as base-64)
     *
     * @return JSON-supported JsonMeta
     */
    public JsonMeta toValidJson() {
        JsonMeta ret = new JsonMeta(markDirtyByDefault);
        for (Map.Entry<String, Object> ent : this.data.entrySet()) {
            if (ent.getValue() == null)
                continue;
            if (ent.getValue() instanceof String)
                ret.set(ent.getKey(), ent.getValue());
            else if (ent.getValue() instanceof Number)
                ret.set(ent.getKey(), ent.getValue());
            else if (ent.getValue() instanceof List)
                ret.set(ent.getKey(), ent.getValue());
            else if (ent.getValue() instanceof JsonMeta)
                ret.set(ent.getKey(), ((JsonMeta) ent.getValue()).toValidJson());
            else {
                GravSerializer ser = new GravSerializer();
                ser.writeObject(ent.getValue());
                ret.set(SERIALIZED_PREFIX + ent.getKey(), ser.toString());
            }
        }
        return ret;
    }

    /**
     * Convert any JSON validifications from {@link JsonMeta#toValidJson()} back to normal java objects
     *
     * @return Parsed JsonMeta
     */
    public JsonMeta fromValidJson() {
        JsonMeta ret = new JsonMeta(markDirtyByDefault);
        for (Map.Entry<String, Object> ent : this.data.entrySet()) {
            if (!ent.getKey().startsWith(SERIALIZED_PREFIX)) {
                ret.set(ent.getKey(), ent.getValue());
                continue;
            }

            if (!(ent.getValue() instanceof String)) {
                throw new IllegalStateException("Found non-string serialization-key");
            }

            String realKey = ent.getKey().substring(SERIALIZED_PREFIX.length());
            String obj = ((String) ent.getValue());
            GravSerializer ser = new GravSerializer(obj);
            Object value = ser.readObject();
            ret.set(realKey, value);
        }
        return ret;
    }

    public String toYaml() {
        return internalYaml(0);
    }

    private String internalYaml(int indentation) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentation; i++) {
            indent.append("  ");
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object val = entry.getValue();
            String valStr = val == null ? "null" :
                    val instanceof JsonMeta ? ((JsonMeta) val).internalYaml(indentation + 1) : val.toString();
            builder.append(indent).append(entry.getKey()).append(": ");
            if (val instanceof JsonMeta) builder.append("\n");
            builder.append(valStr);
            if (!(val instanceof JsonMeta)) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private static String jsonStringify(Object obj) {
        String valStr;
        if (obj == null) {
            valStr = "null";
        } else if (obj instanceof JsonMeta) {
            valStr = ((JsonMeta) obj).toJson();
        } else if (obj instanceof List) {
            valStr = "[" + ((List<?>) obj).stream().map(JsonMeta::jsonStringify)
                    .collect(Collectors.joining(", ")) + "]";
        } else if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else {
            valStr = obj.toString();
        }
        return valStr;
    }

    public JsonMeta reduce() {
        lock.lock();
        try {
            JsonMeta meta = new JsonMeta();

            //Iterate through updated fields
            for (String updatedField : record.getUpdatedFields()) {
                Object val = get(updatedField);

                //Check if it's a JsonMeta with it's parent as us
                if (val instanceof JsonMeta && ((JsonMeta) val).parent == this) {
                    meta.data.put(updatedField, ((JsonMeta) val).reduce());
                } else {
                    meta.data.put(updatedField, val);
                }
            }
            return meta;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void serialize(GravSerializer serializer) {
        serialize(serializer, false);
    }

    public void serialize(GravSerializer serializer, boolean reduced) {
        lock.lock();
        try {
            serializer.writeByte((byte) 2);

            serializer.writeInt(FORMAT_VERSION);

            int len = reduced ? record.updatedFields.size() : this.data.size();

            serializer.writeBoolean(reduced); //Probably not needed but who cares about an extra couple bytes
            serializer.writeInt(len);

            if (reduced) {
                for (String updatedField : record.getUpdatedFields()) {
                    Object val = get(updatedField);
                    serializeObject(serializer, updatedField, val, true);
                }
            } else {
                for (Map.Entry<String, Object> entry : this.data.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    serializeObject(serializer, key, val, false);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void serializeObject(GravSerializer serializer, String key, Object val, boolean reduced) {
        if (val instanceof JsonMeta && ((JsonMeta) val).parent == this) { //If it's parent isn't us, then treat it like any other object
            serializer.writeByte((byte) 0);
            serializer.writeString(key);
            ((JsonMeta) val).serialize(serializer, reduced);
        } else {
            serializer.writeByte((byte) 1);
            serializer.writeString(key);
            serializer.writeMark();
            serializer.writeInt(0);
            int pos = serializer.size();
            serializer.writeObject(val);
            pos = serializer.size() - pos;
            serializer.writeReset();
            serializer.writeInt(pos);
            serializer.writeReset();
        }
    }

    public static JsonMeta deserialize(GravSerializer serializer) {
        JsonMeta meta = new JsonMeta();

        //No need to use lock
        byte disc = serializer.readByte();

        int version = 1;
        boolean reduced = false;

        if (disc == 2) {
            version = serializer.readInt();
            reduced = serializer.readBoolean();
        } else {
            reduced = disc == 1;
        }

        int len = serializer.readInt();

        for (int i = 0; i < len; i++) {
            byte type = serializer.readByte();
            String key = serializer.readString();
            Object object;
            if (type == 0) {
                object = JsonMeta.deserialize(serializer);
                link(meta, (JsonMeta) object, key); // Bug fix: parent was not being set on deserialization.
            } else {
                int sl = -1;
                if (version >= 2)
                    sl = serializer.readInt();

                serializer.mark();
                try {
                    object = serializer.readObject();
                } catch (ObjectDeserializationException ex) {
                    if (version >= 2) {
                        serializer.reset();
                        meta.toDeserialize.put(key, new GravSerializer(serializer.readBytes(sl)));
                        continue;
                    } else {
                        throw new UnsupportedOperationException("Cannot create toDeserialize for data serialized before version 2.");
                    }
                }
            }
            if (object != null) {
                meta.data.put(key, object);
            }
        }

        return meta;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<>();
        this.data.forEach((k, v) -> {
            if (v instanceof JsonMeta) {
                ret.put(k, ((JsonMeta) v).asMap());
            } else {
                ret.put(k, v);
            }
        });
        return ret;
    }

    public Meta toMeta() {
        return new Meta(asMap());
    }

    public static JsonMeta fromJson(String str) {
        return JsonUtil.readJson(str);
    }
}
