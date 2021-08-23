package net.ultragrav.serializer;

import net.ultragrav.serializer.util.JsonUtil;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

//TODO make thread-safe
public class JsonMeta implements GravSerializable {

    private final Map<String, Object> data = new HashMap<>();

    private String delimiter = "\\.";
    private String[] path = new String[0];
    private JsonMeta parent = null;

    private volatile ReentrantLock lock = new ReentrantLock();

    private volatile boolean autoDirt = false;

    private final JsonMetaUpdateRecord record = new JsonMetaUpdateRecord();

    public JsonMeta() {
    }

    public JsonMeta(boolean useRecord) {
        this.autoDirt = useRecord;
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

    public boolean isAutoDirt() {
        return autoDirt;
    }

    /**
     * Whether to default to markDirt = true on operations.
     */
    public void setAutoDirt(boolean autoDirt) {
        this.autoDirt = autoDirt;
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

    public <T> T get(String path) {
        return get(path.split(delimiter));
    }

    public <T> T get(String... path) {
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
                    return (T) o;
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

    public <T> T getOrSet(String path, T defaultValue) {
        return getOrSet(path, defaultValue, autoDirt);
    }

    public <T> T getOrSet(String path, T defaultValue, boolean markDirty) {
        lock.lock();
        try {
            T obj = get(path);
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

    public void set(String path, Object value) {
        set(path.split(delimiter), value);
    }

    public void set(String path, Object value, boolean markDirty) {
        set(path.split(delimiter), value, markDirty);
    }

    public void set(String[] path, Object value) {
        set(path, value, autoDirt);
    }

    /**
     * Set a key to a value. Using markDirty=false will be faster.
     */
    public void set(String[] path, Object value, boolean markDirty) { //Doesn't use recursion, may change later
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
                        value = new JsonMeta((Map<String, Object>) value);
                    }
                    if (value instanceof Meta) {
                        value = new JsonMeta(((Meta) value).asMap());
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

                    return;
                }

                if (!(o instanceof JsonMeta)) {
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
        builder.append("{");
        String objs = data.entrySet().stream()
                .map(ent -> "\"" + ent.getKey() + "\": " + jsonStringify(ent.getValue()))
                .collect(Collectors.joining(", "));
        builder.append(objs);
        return builder.append("}").toString();
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
            serializer.writeObject(val);
        }
    }

    public static JsonMeta deserialize(GravSerializer serializer) {
        JsonMeta meta = new JsonMeta();

        //No need to use lock

        boolean reduced = serializer.readBoolean();
        int len = serializer.readInt();

        for (int i = 0; i < len; i++) {
            byte type = serializer.readByte();
            String key = serializer.readString();
            Object object;
            if (type == 0) {
                object = JsonMeta.deserialize(serializer);
            } else {
                object = serializer.readObject();
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

    public static void main(String[] args) {
        JsonMeta meta = new JsonMeta();
        long ms = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            meta.set(new String[] {"" + i}, i, false);
        }
        ms = System.currentTimeMillis() - ms;
        System.out.println(ms);

        ms = System.currentTimeMillis();

        GravSerializer serializer = new GravSerializer();
        for (int i = 0; i < 1000000; i++) {
            serializer.writeString("1");
            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);

            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);

            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);

            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);

            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);

            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);

            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);

            serializer.writeByte((byte) 0);
            serializer.writeByte((byte) 2);
            serializer.writeInt(1);
        }
        ms = System.currentTimeMillis() - ms;
        meta.serialize(serializer);
        System.out.println(ms);
    }
}
