package net.ultragrav.serializer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ObjectDeserializationException extends RuntimeException {

    public enum DeserializationExceptionCause {
        NO_DESERIALIZATION_METHOD;
    }

    private int cause;
    public ObjectDeserializationException(String message, int cause) {
        super(message);
    }
}
