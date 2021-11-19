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
            try {
                Method m = ReflectionUtil.getCompatibleMethod(clazz, "deserialize", args);
                if (m == null) throw new NoSuchMethodException();
                return (T) m.invoke(null, meta);
            } catch (NoSuchMethodException ignored) {
            } catch (NullPointerException e) {
                c1 = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            Constructor<T> constructor = ReflectionUtil.getCompatibleConstructor(clazz, args);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            if (!c1)
                throw new ObjectDeserializationException("ERROR: Could NOT find a deserialization method for " + className, e, ObjectDeserializationException.DeserializationExceptionCause.NO_DESERIALIZATION_METHOD);
            throw new ObjectDeserializationException("ERROR: Deserialization method non-static for " + className, e, ObjectDeserializationException.DeserializationExceptionCause.NO_DESERIALIZATION_METHOD);
        }
    }

    JsonMeta serialize();

    default void serialize(GravSerializer serializer) {
        serialize().serialize(serializer);
    }
}
