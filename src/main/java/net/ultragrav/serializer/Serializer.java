package net.ultragrav.serializer;

public interface Serializer<T> {
    void serialize(GravSerializer serializer, Object t);
    T deserialize(GravSerializer serializer, Object... args);
}
