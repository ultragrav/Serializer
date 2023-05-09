package net.ultragrav.serializer;

import net.ultragrav.serializer.util.JsonUtil;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JsonMeta implements GravSerializable {
    private static final int FORMAT_VERSION = 2;
    private static final String CLASS_FIELD = "__class";
    private static final String SERIALIZED_PREFIX = "__##";

    private final Map<String, Object> data = new HashMap<>();
    public final Map<String, GravSerializer> toDeserialize = new HashMap<>();

    private String delimiter = "\\.";
    private String[] path = new String[0];
    private volatile JsonMeta parent = null;

    private final RefocusableLock lock = new RefocusableLock(this);

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

    @Deprecated
    public JsonMeta(Map<String, Object> map) {
        for (Map.Entry<String, Object> ent : map.entrySet()) {
            set(ent.getKey(), ent.getValue(), false);
        }
    }

    public JsonMeta(String delimiter) {
        this.delimiter = delimiter;
    }

    public JsonMeta getParent() {
        return parent;
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

    public RefocusableLock getLock() {
        return lock;
    }

    public <T> T get(String path, Object... constructionArgs) {
        return get(path.split(delimiter), constructionArgs);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String path) {
        try {
            return this.get(path);
        } catch (ClassCastException ex) {
            return (Map<K, V>) this.<JsonMeta>get(path).asShallowMap();
        }
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMapOrDefault(String path, Map<K, V> def) {
        Map<K, V> map;
        try {
            map = this.get(path);
        } catch (ClassCastException ex) {
            map = (Map<K, V>) this.<JsonMeta>get(path).asShallowMap();
        }

        if (map == null) return def;
        return map;
    }

    @SuppressWarnings("unchecked")
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
                            ser.mark();
                            try {
                                o = ser.readObject(constructionArgs);
                                current.toDeserialize.remove(s);
                                if (o != null) {
                                    current.data.put(s, o);
                                }
                            } catch (Exception e) {
                                ser.reset();
                                throw new RuntimeException("Failed to deserialize object: " + String.join(".", path), e);
                            }
                        }
                    }
                    return (T) o;
                }
            }
            return (T) current;
        } finally {
            lock.unlock();
        }
    }

    public boolean has(String path) {
        return has(path.split(delimiter));
    }

    public boolean has(String[] path) {
        lock.lock();
        try {
            JsonMeta current = this;
            for (int i = 0, pathLength = path.length; i < pathLength - 1; i++) {
                String s = path[i];
                Object o = current.data.get(s);
                if (o instanceof JsonMeta) {
                    current = (JsonMeta) o;
                } else {
                    return false;
                }
            }
            return current.data.containsKey(path[path.length - 1])
                    || current.toDeserialize.containsKey(path[path.length - 1]);
        } finally {
            lock.unlock();
        }
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

    /**
     * Return the value represented by path if it exists, otherwise return defaultValue
     *
     * @param path             Path to value
     * @param defaultValue     Default Value
     * @param constructionArgs Construction arguments
     * @param <T>              Type of value
     * @return Value or default
     */
    public <T> T getOrDefault(String path, T defaultValue, Object... constructionArgs) {
        lock.lock();
        try {
            T obj = get(path, constructionArgs);
            if (obj == null) {
                return defaultValue;
            }
            return obj;
        } finally {
            lock.unlock();
        }
    }

    public <T> T getOrCompute(String path, Supplier<T> defaultProvider, Object... constructionArgs) {
        return getOrCompute(path, defaultProvider, markDirtyByDefault, constructionArgs);
    }

    public <T> T getOrCompute(String path, Supplier<T> defaultProvider, boolean markDirty, Object... constructionArgs) {
        lock.lock();
        try {
            T obj = get(path, constructionArgs);
            if (obj == null) {
                set(path, (obj = defaultProvider.get()), markDirty);
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

    public <T> JsonMeta setSerializedList(String path, Iterable<T> list, Function<T, JsonMeta> serializer) {
        List<JsonMeta> serializedList = new ArrayList<>();
        for (T t : list) {
            serializedList.add(serializer.apply(t));
        }
        set(path, serializedList);
        return this;
    }

    public <T> List<T> getSerializedList(String path, Function<JsonMeta, T> deserializer) {
        List<JsonMeta> serializedList = get(path);
        if (serializedList == null)
            return null;
        List<T> list = new ArrayList<>();
        for (JsonMeta m : serializedList) {
            list.add(deserializer.apply(m));
        }
        return list;
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

                    // Setting the value on current now.
                    Object prev;

//                    if (value instanceof Map) {
//                        Map<?, ?> vMap = (Map<?, ?>) value;
//                        if (vMap.size() == 0) {
//                            value = new JsonMeta();
//                        } else {
//                            JsonMeta submeta = new JsonMeta();
//                            for (Map.Entry<?, ?> ent : vMap.entrySet()) {
//                                if (ent.getKey() instanceof String) {
//                                    submeta.set((String) ent.getKey(), ent.getValue());
//                                } else {
//                                    submeta = null;
//                                    break;
//                                }
//                            }
//                            if (submeta != null)
//                                value = submeta;
//                        }
//                    }
                    if (value instanceof Meta) {
                        value = ((Meta) value).toJsonMeta();
                    }

                    if (value == null) {

                        // Remove the key
                        prev = current.data.remove(s);

                    } else {
                        prev = current.data.put(s, value);
                    }

                    // Get rid of toDeserialize
                    current.toDeserialize.remove(s);

                    if (markDirty) {
                        current.markDirty0(s);
                    }

                    //Link it if it's a JsonMeta
                    if (value instanceof JsonMeta) {
                        JsonMeta v = (JsonMeta) value;
                        link(current, v, s);

                        if (markDirty) {
                            if (prev instanceof JsonMeta) {

                                // Preserve the updated status of updated fields on the previous JsonMeta
                                List<String> currentlyDirty = ((JsonMeta) prev).getRecord().getUpdatedFields();

                                v.getRecord().clear();
                                v.markDirtyDiff((JsonMeta) prev);

                                currentlyDirty.forEach(v::markDirty);

                            } else {

                                // Don't think this is necessary but I don't want to remove it
                                v.getRecord().clear();

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
            // Go through all data elements
            for (String key : meta.getKeys()) {

                Object next = meta.data.get(key);
                Object current = data.get(key);

                // If we are writing from JsonMeta to JsonMeta, pass down the call, so we don't
                // unnecessarily mark fields as dirty
                if (next instanceof JsonMeta && current instanceof JsonMeta) {
                    ((JsonMeta) current).putAll((JsonMeta) next, markDirty);
                } else {
                    set(new String[]{key}, next, markDirty);
                }
            }

            // Since toDeserialize can never contain JsonMetas, we can just dump all the elements
            this.toDeserialize.putAll(meta.toDeserialize);
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

        parent.lock.lock();

        child.lock.lock();

        child.setMarkDirtyByDefaultRecursive(parent.markDirtyByDefault);

        // Switch to the new lock
        Lock current = child.lock.getCurrentLock();
        child.parent = parent;
        current.unlock();

        // Set paths
        child.path = currentPath;

        // Set record's children
        parent.record.children.add(child.record);

        // Unlock

        parent.lock.unlock();
    }

    public void markDirty(String path) {
        markDirty(path.split(delimiter));
    }

    public void markDirty(String[] path) {
        String[] pathMinusOne = new String[path.length - 1];
        System.arraycopy(path, 0, pathMinusOne, 0, path.length - 1);
        JsonMeta meta = get(pathMinusOne);
        if (meta == null)
            throw new IllegalArgumentException();

        if (meta.get(path[path.length - 1]) instanceof JsonMeta) {
            ((JsonMeta) meta.get(path[path.length - 1])).markDirtyRecursive();
        } else {
            meta.markDirty0(path[path.length - 1]);
        }
    }

    private void markDirty0(String key) {
        record.markDirty(key);
        if (parent != null) {
            if (path.length == 0) throw new IllegalStateException("This shouldn't happen.");
            parent.markDirty0(path[path.length - 1]);
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
            parent.markDirty0(path[path.length - 1]);
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
            parent.markDirty0(path[path.length - 1]);
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
                    val instanceof JsonMeta ? ((JsonMeta) val).recursiveFullToString(indentation + 1) : val.toString();
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

    public String toStringSerialize() {
        return recursiveToStringSerialize(0);
    }

    private String recursiveToStringSerialize(int indentation) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentation; i++) {
            indent.append("  ");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{").append("\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object val = entry.getValue();
            String valStr;
            if (val == null) valStr = "null";
            else if (val instanceof JsonMeta) valStr = ((JsonMeta) val).recursiveToStringSerialize(indentation + 1);
            else if (val instanceof JsonMetaSerializable)
                valStr = val.getClass() + "* " + ((JsonMetaSerializable) val).serialize().recursiveToStringSerialize(indentation + 1);
            else valStr = val.toString();
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
        lock.lock();
        try {
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
                else if (ent.getValue() instanceof JsonMetaSerializable) {
                    JsonMetaSerializable ser = (JsonMetaSerializable) ent.getValue();
                    JsonMeta meta = ser.serialize();
                    meta.set(CLASS_FIELD, ent.getValue().getClass().getName());
                    ret.set(ent.getKey(), meta.toValidJson());
                } else if (ent.getValue() instanceof Boolean) {
                    ret.set(ent.getKey(), ent.getValue());
                } else {
                    GravSerializer ser = new GravSerializer();
                    ser.writeObject(ent.getValue());
                    ret.set(SERIALIZED_PREFIX + ent.getKey(), ser.toString());
                }
            }

            // toDeserialize
            for (Map.Entry<String, GravSerializer> toD : toDeserialize.entrySet()) {
                ret.set(SERIALIZED_PREFIX + toD.getKey(), toD.getValue().toString());
            }

            return ret;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Convert any JSON validifications from {@link JsonMeta#toValidJson()} back to normal java objects
     *
     * @return Parsed JsonMeta
     */
    public JsonMeta fromValidJson() {
        lock.lock();
        try {
            JsonMeta ret = new JsonMeta(markDirtyByDefault);
            for (Map.Entry<String, Object> ent : this.data.entrySet()) {
                if (ent.getValue() instanceof JsonMeta) {
                    JsonMeta meta = (JsonMeta) ent.getValue();
                    if (meta.has(CLASS_FIELD)) {
                        String className = meta.get(CLASS_FIELD);
                        try {
                            Class<? extends JsonMetaSerializable> clazz = Class.forName(className)
                                    .asSubclass(JsonMetaSerializable.class);
                            Object obj = JsonMetaSerializable.deserializeObject(clazz, meta);
                            ret.set(ent.getKey(), obj);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ret.set(ent.getKey(), meta.fromValidJson());
                    }
                    continue;
                }

                if (!ent.getKey().startsWith(SERIALIZED_PREFIX)) {
                    ret.set(ent.getKey(), ent.getValue());
                    continue;
                }

                if (!(ent.getValue() instanceof String)) {
                    throw new IllegalStateException("Found non-string serialization-key");
                }

                String realKey = ent.getKey().substring(SERIALIZED_PREFIX.length());
                String obj = (String) ent.getValue();

                GravSerializer ser = new GravSerializer(obj);
                ser.mark();

                try {
                    Object value = ser.readObject();
                    if (value != null) {
                        ret.set(realKey, value);
                    }
                } catch (ObjectDeserializationException e) {
                    // Add to toDeserialize
                    ser.reset();
                    ret.toDeserialize.put(realKey, ser);
                }

            }
            return ret;
        } finally {
            lock.unlock();
        }
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
            return "\"" + jsonEscape((String) obj) + "\"";
        } else {
            valStr = obj.toString();
        }
        return valStr;
    }

    private static String jsonEscape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        return escaped;
    }


    public JsonMeta reduce() {
        lock.lock();
        try {
            JsonMeta meta = new JsonMeta();

            //Iterate through updated fields
            for (String updatedField : record.getUpdatedFields()) {
                Object val = get(updatedField);

                //Check if it's a JsonMeta with its parent as us
                if (val instanceof JsonMeta && ((JsonMeta) val).parent == this) {
                    JsonMeta reduced = ((JsonMeta) val).reduce();
                    // link
                    link(meta, reduced, updatedField);
                    meta.data.put(updatedField, reduced);
                } else {
                    meta.data.put(updatedField, val); // MUST use meta.data.put otherwise null values will just be ignored
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

            int len = reduced ? record.updatedFields.size() : (this.data.size() + this.toDeserialize.size());

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
                for (Map.Entry<String, GravSerializer> entry : this.toDeserialize.entrySet()) {
                    serializer.writeByte((byte) 1);
                    serializer.writeString(entry.getKey());
                    serializer.writeByteArray(entry.getValue().toByteArray());
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

            int sizePos = serializer.getWritePosition();

            serializer.writeInt(0);

            int pos = serializer.size();

            serializer.writeObject(val);

            int size = serializer.size() - pos; // Could be replaced by end - pos.

            int end = serializer.getWritePosition();

            serializer.setWritePosition(sizePos);
            serializer.writeInt(size);
            serializer.setWritePosition(end);
        }
    }

    public static JsonMeta deserialize(GravSerializer serializer) {
        return deserialize(serializer, true);
    }

    public static JsonMeta deserialize(GravSerializer serializer, boolean doDeserialization) {
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
            Object object = null;
            if (type == 0) {
                object = JsonMeta.deserialize(serializer, doDeserialization);
                link(meta, (JsonMeta) object, key); // Bug fix: parent was not being set on deserialization.
            } else if (type == 1) {
                int sl = -1;
                if (version >= 2)
                    sl = serializer.readInt();

                if (doDeserialization) {
                    int markedPos = serializer.getReadPosition();
                    try {
                        object = serializer.readObject();
                    } catch (ObjectDeserializationException ex) {
                        if (version >= 2) {
                            serializer.setReadPosition(markedPos);
                            meta.toDeserialize.put(key, new GravSerializer(serializer.readBytes(sl)));
                            continue;
                        } else {
                            throw new UnsupportedOperationException("Cannot create toDeserialize for data serialized before version 2.");
                        }
                    }
                } else {
                    serializer.reset();
                    meta.toDeserialize.put(key, new GravSerializer(serializer.readBytes(sl)));
                    continue;
                }
            }
            meta.data.put(key, object);
        }

        return meta;
    }

    public Map<String, Object> asShallowMap() {
        return new HashMap<>(this.data);
    }

    public Map<String, Object> asMap() {
        lock.lock();
        try {
            Map<String, Object> ret = new HashMap<>();
            this.data.forEach((k, v) -> {
                if (v instanceof JsonMeta) {
                    ret.put(k, ((JsonMeta) v).asMap());
                } else {
                    ret.put(k, v);
                }
            });
            return ret;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Object> asFlatMap() {
        lock.lock();
        try {
            Map<String, Object> ret = new HashMap<>();
            this.data.forEach((k, v) -> {
                if (v instanceof JsonMeta) {
                    Map<String, Object> fm = ((JsonMeta) v).asFlatMap();
                    fm.forEach((k2, v2) -> ret.put(k + '.' + k2, v2));
                } else {
                    ret.put(k, v);
                }
            });
            return ret;
        } finally {
            lock.unlock();
        }
    }

    public Meta toMeta() {
        return new Meta(asMap());
    }

    public static JsonMeta fromJson(String str) {
        return JsonUtil.readJson(str);
    }

    public JsonMeta copy() {
        JsonMeta ret = new JsonMeta();
        lock.lock();
        try {
            for (String key : getKeys()) {
                Object val = get(key);
                if (val instanceof JsonMeta) {
                    ret.set(key, ((JsonMeta) val).copy());
                } else {
                    ret.set(key, val);
                }
            }
        } finally {
            lock.unlock();
        }
        return ret;
    }

    public JsonMeta toBson() {
        lock.lock();
        try {
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
                    ret.set(ent.getKey(), ((JsonMeta) ent.getValue()).toBson());
                else if (ent.getValue() instanceof JsonMetaSerializable) {
                    JsonMetaSerializable ser = (JsonMetaSerializable) ent.getValue();
                    JsonMeta meta = ser.serialize();
                    meta.set(CLASS_FIELD, ent.getValue().getClass().getName());
                    ret.set(ent.getKey(), meta.toBson());
                } else if (ent.getValue() instanceof Boolean) {
                    ret.set(ent.getKey(), ent.getValue());
                } else {
                    GravSerializer ser = new GravSerializer();
                    ser.writeObject(ent.getValue());
                    ret.set(SERIALIZED_PREFIX + ent.getKey(), ser.toByteArray());
                }
            }

            // toDeserialize
            for (Map.Entry<String, GravSerializer> toD : toDeserialize.entrySet()) {
                ret.set(SERIALIZED_PREFIX + toD.getKey(), toD.getValue().toByteArray());
            }
            return ret;
        } finally {
            lock.unlock();
        }

    }

    public JsonMeta fromBson() {

        lock.lock();
        try {
            JsonMeta ret = new JsonMeta(markDirtyByDefault);
            for (Map.Entry<String, Object> ent : this.data.entrySet()) {
                if (ent.getValue() instanceof JsonMeta) {
                    JsonMeta meta = (JsonMeta) ent.getValue();
                    if (meta.has(CLASS_FIELD)) {
                        String className = meta.get(CLASS_FIELD);
                        try {
                            Class<? extends JsonMetaSerializable> clazz = Class.forName(className)
                                    .asSubclass(JsonMetaSerializable.class);
                            Object obj = JsonMetaSerializable.deserializeObject(clazz, meta);
                            ret.set(ent.getKey(), obj);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ret.set(ent.getKey(), meta.fromBson());
                    }
                    continue;
                }

                if (!ent.getKey().startsWith(SERIALIZED_PREFIX)) {
                    ret.set(ent.getKey(), ent.getValue());
                    continue;
                }

                if (!(ent.getValue() instanceof byte[])) {
                    throw new IllegalStateException("Found non-byte-array serialization value");
                }

                String realKey = ent.getKey().substring(SERIALIZED_PREFIX.length());
                byte[] obj = (byte[]) ent.getValue();

                GravSerializer ser = new GravSerializer(obj);
                ser.mark();

                try {
                    Object value = ser.readObject();
                    if (value != null) {
                        ret.set(realKey, value);
                    }
                } catch (ObjectDeserializationException e) {
                    // Add to toDeserialize
                    ser.reset();
                    ret.toDeserialize.put(realKey, ser);
                }

            }
            return ret;
        } finally {
            lock.unlock();
        }
    }
}
