package net.ultragrav.serializer;

import net.ultragrav.serializer.util.Json;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Meta implements GravSerializable {
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private Map<String, Object> meta = new HashMap<>();
    private Map<String, GravSerializer> toDeserialize = new HashMap<>();

    public Meta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public Meta() {
    }

    /**
     * Load this element meta from a serializer
     *
     * @param serializer Serializer
     */
    public Meta(GravSerializer serializer) {
        if (serializer == null)
            return;
        int amount = serializer.readInt();
        for (int i = 0; i < amount; i++) {
            String key = serializer.readString();
            GravSerializer wrapper = serializer.readSerializer();
            try {
                wrapper.mark();
                Object object = wrapper.readObject();
                meta.put(key, object);
            } catch (ObjectDeserializationException e) {
                if (e.getDeserializationCause().equals(ObjectDeserializationException.DeserializationExceptionCause.NO_DESERIALIZATION_METHOD)) {
                    wrapper.reset();
                    toDeserialize.put(key, wrapper);
                }
            }
        }
    }

    /**
     * Create a meta from a json string
     *
     * @param json Json String
     * @return Meta
     */
    public static Meta fromJson(String json) {
        return new Meta(Json.read(json).asMap());
    }

    public Meta getMeta(String key) {
        Map<String, Object> data = this.getObject(key);
        return data == null ? null : new Meta(data);
    }

    /**
     * Same as {@link Meta#getOrSet(String, Object, Object...)} but specifically for
     * for Meta
     *
     * @param key          Key
     * @param defaultValue Default Meta
     * @return Existing value if present, otherwise {@code defaultValue}
     */
    public Meta getOrSetMeta(String key, Meta defaultValue) {
        //Read lock is less expensive, so try first
        lock.readLock().lock();
        try {
            Meta meta = getMeta(key);
            if (meta != null)
                return meta;
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            Meta meta = getMeta(key);
            if (meta == null) {
                set(key, defaultValue);
                return defaultValue;
            }
            return meta;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Set a value in this meta.
     *
     * @param key    Key
     * @param object New value
     */
    public void set(String key, Object object) {
        lock.writeLock().lock();
        try {
            if (object instanceof Meta) {
                this.meta.put(key, ((Meta) object).meta);
            } else {
                this.meta.put(key, object);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Put all fields from {@code other} into this meta.
     *
     * @param other Other meta
     */
    public void setAll(Meta other) {
        this.meta.putAll(other.meta);
    }

    /**
     * Get an object from this element meta
     *
     * @param key              Key
     * @param constructionArgs if the object could not be constructed during deserialization, it will be attempt to be constructed again with these arguments
     * @return Object
     */
    public <T> T get(String key, Object... constructionArgs) {
        lock.readLock().lock();
        Object o;
        try {
            o = this.meta.get(key);
        } finally {
            lock.readLock().unlock();
        }
        if (o == null) {
            lock.readLock().lock();
            try {
                if (this.toDeserialize.containsKey(key)) {
                    GravSerializer serializer = toDeserialize.get(key);
                    o = serializer.readObject(constructionArgs); //Exception might be thrown here
                    toDeserialize.remove(key);
                }
            } finally {
                lock.readLock().unlock();
            }
            if (o != null)
                this.set(key, o);
            return (T) o;
        }
        return (T) o;
    }

    /**
     * Get a value, or set and get if absent.
     * <p>
     * If the value is already present, it is returned, otherwise the default
     * value is set and returned.
     *
     * @param key              Key
     * @param defaultValue     Default value
     * @param constructionArgs Construction arguments (for deserialization)
     * @param <T>              Type of the value
     * @return Existing value if present, otherwise {@code defaultValue}
     */
    public <T> T getOrSet(String key, T defaultValue, Object... constructionArgs) {
        lock.readLock().lock();
        try {
            Object object = get(key, constructionArgs);
            if (object != null)
                return (T) object;
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            Object object = get(key, constructionArgs);
            if (object == null) {
                set(key, defaultValue);
                return defaultValue;
            }
            return (T) object;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get an object, this is identical to {@link Meta#get(String, Object...)}
     *
     * @param key       Key
     * @param arguments Construction arguments (for deserialization)
     * @param <T>       Type of the value
     * @return Value
     * @deprecated Use {@link Meta#get(String, Object...)} instead
     */
    @Deprecated
    public <T> T getObject(String key, Object... arguments) {
        return get(key, arguments);
    }

    /**
     * Serialize this element meta into a serializer
     *
     * @param serializer Serializer
     */
    @Override
    public void serialize(GravSerializer serializer) {
        lock.writeLock().lock();
        try {
            serializer.writeInt(meta.size() + toDeserialize.size());
            meta.forEach((k, o) -> {
                GravSerializer wrapper = new GravSerializer();
                wrapper.writeObject(o);
                serializer.writeString(k);
                serializer.writeSerializer(wrapper);
            });
            toDeserialize.forEach((k, o) -> {
                serializer.writeString(k);
                serializer.writeSerializer(o);
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get a string representation of this meta data
     * Uses recursion with indentation to show a
     * structural representation of this meta.
     *
     * @return String representation of this meta
     */
    @Override
    public String toString() {
        return recursiveToString(0);
    }

    /**
     * Recursively generate the string representation of this meta.
     *
     * @param indentation Indentation to use
     * @return String representation of this meta
     */
    private String recursiveToString(int indentation) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentation; i++) {
            indent.append("  ");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{").append("\n");
        for (Map.Entry<String, Object> entry : meta.entrySet()) {
            Object val = entry.getValue();
            String valStr = val instanceof Meta ? ((Meta) val).recursiveToString(indentation + 1) : val.toString();
            builder.append(indent).append("  ").append(entry.getKey()).append(": ").append(valStr).append("\n");
        }

        return builder.append(indent).append("}").toString();
    }

    /**
     * Returns a Meta containing all fields that are
     * present in {@code other}, but not present, or
     * different in this meta.
     *
     * @param other Other Meta
     * @return Differences between this and other
     */
    public Meta diff(Meta other) {
        Meta ret = new Meta();
        for (Map.Entry<String, Object> ent : other.meta.entrySet()) {
            if (!meta.containsKey(ent.getKey())) {
                ret.set(ent.getKey(), ent.getValue());
                continue;
            }
            Object oObj = ent.getValue();
            Object tObj = meta.get(ent.getKey());

            if (!Objects.equals(oObj, tObj)) {
                ret.set(ent.getKey(), ent.getValue());
            }
        }
        return ret;
    }

    /**
     * Get a copy of this meta's internal data
     * <p>
     * Note: This does not contain any objects that failed deserialization during the initialization
     *
     * @return Copy of the internal data
     */
    public Map<String, Object> asMap() {
        return new HashMap<>(meta);
    }

    /**
     * Represent this meta as a JSON string
     *
     * @return JSON String
     */
    public String asJson() {
        return Json.make(meta).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meta meta1 = (Meta) o;
        return meta.equals(meta1.meta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meta);
    }
}