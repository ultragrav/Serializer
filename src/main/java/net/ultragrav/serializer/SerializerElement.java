package net.ultragrav.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

public class SerializerElement {
    private Class<?> clazz;
    private final Serializer<?> serializer;

    public SerializerElement(Class<?> clazz, Serializer<?> serializer1) {
        this.clazz = clazz;
        serializer = serializer1;
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    public Serializer<?> getSerializer() {
        return serializer;
    }


}
