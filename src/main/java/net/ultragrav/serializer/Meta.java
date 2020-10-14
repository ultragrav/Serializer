package net.ultragrav.serializer;

import com.google.common.annotations.Beta;
import net.ultragrav.serializer.util.Json;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Meta implements GravSerializable {
    private Map<String, Object> meta = new HashMap<>();
    private Map<String, GravSerializer> toDeserialize = new HashMap<>();

    public Meta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public Meta() {}

    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

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

    public Meta getMeta(String key) {
        return new Meta(this.<Map<String, Object>>getObject(key));
    }

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

    public <T> T getOrSet(String key, T defaultValue, Object... constructionArgs) {
        lock.readLock().lock();
        try {
            Object object = get(key, constructionArgs);
            if(object != null)
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

    public String asJson() {
        return Json.make(meta).toString();
    }

    public static Meta fromJson(String json) {
        return new Meta(Json.read(json).asMap());
    }
}