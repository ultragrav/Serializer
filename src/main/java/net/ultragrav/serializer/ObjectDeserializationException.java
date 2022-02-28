package net.ultragrav.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ObjectDeserializationException extends RuntimeException {

    public enum DeserializationExceptionCause {
        NO_DESERIALIZATION_METHOD,
        CLASS_NOT_FOUND,
        INTERNAL,
        UNKNOWN
    }

    private DeserializationExceptionCause cause;
    public ObjectDeserializationException(String message, Throwable cause1, DeserializationExceptionCause cause) {
        super(message, cause1);
        this.cause = cause;
    }

    public ObjectDeserializationException(String message, DeserializationExceptionCause cause) {
        super(message);
        this.cause = cause;
    }

    public DeserializationExceptionCause getDeserializationCause() {
        return cause;
    }
}
