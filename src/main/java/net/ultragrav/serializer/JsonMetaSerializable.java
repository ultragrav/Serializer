package net.ultragrav.serializer;

import net.ultragrav.serializer.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Requires either a static method deserialize(JsonMeta) OR a constructor(JsonMeta)
 */
public interface JsonMetaSerializable extends GravSerializable {
    static <T extends JsonMetaSerializable> T deserializeObject(Class<T> clazz, GravSerializer serializer, Object... otherArguments) {
        return deserializeObject(clazz, JsonMeta.deserialize(serializer), otherArguments);
    }

    static <T extends JsonMetaSerializable> T deserializeObject(Class<T> clazz, JsonMeta meta, Object... otherArguments) {
        String className = clazz.getName();

        Object[] args = new Object[otherArguments.length + 1];
        args[0] = meta;
        System.arraycopy(otherArguments, 0, args, 1, otherArguments.length);

        boolean c1 = false;
        try {
            Method m = ReflectionUtil.getCompatibleMethod(clazz, "deserialize", args);
            if (m == null) throw new NoSuchMethodException();
            return (T) m.invoke(null, meta);
        } catch (NoSuchMethodException ignored) {
        } catch (NullPointerException e) {
            c1 = true;
        } catch (Exception e) {
            throw new ObjectDeserializationException("An unknown error occurred while deserializing " + className, e, ObjectDeserializationException.DeserializationExceptionCause.UNKNOWN);
        }

        Constructor<T> constructor = ReflectionUtil.getCompatibleConstructor(clazz, args);

        if (constructor == null) {
            if (c1)
                throw new ObjectDeserializationException("Deserialization method non-static for " + className, ObjectDeserializationException.DeserializationExceptionCause.NO_DESERIALIZATION_METHOD);
            throw new ObjectDeserializationException("Could not find a deserialization method for " + className, ObjectDeserializationException.DeserializationExceptionCause.NO_DESERIALIZATION_METHOD);
        }

        try {
            return (T) constructor.newInstance(args);
        } catch(Exception e) {
            throw new ObjectDeserializationException("An error occurred while initializing " + className, e, ObjectDeserializationException.DeserializationExceptionCause.INTERNAL);
        }
    }

    JsonMeta serialize();

    default void serialize(GravSerializer serializer) {
        serialize().serialize(serializer);
    }
}
