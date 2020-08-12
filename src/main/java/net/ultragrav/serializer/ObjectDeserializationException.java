package net.ultragrav.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ObjectDeserializationException extends RuntimeException {

    public enum DeserializationExceptionCause {
        NO_DESERIALIZATION_METHOD;
    }

    private DeserializationExceptionCause cause;
    public ObjectDeserializationException(String message, DeserializationExceptionCause cause) {
        super(message);
        this.cause = cause;
    }

    public DeserializationExceptionCause getDeserializationCause() {
        return cause;
    }
}
