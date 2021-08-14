package net.ultragrav.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Requires either a static method deserialize(Meta) OR a constructor(Meta)
 */
public interface MetaSerializable extends GravSerializable {
    static Object deserializeObject(Class<? extends MetaSerializable> clazz, GravSerializer serializer) {
        return deserializeObject(clazz, new Meta(serializer));
    }

    static <T extends MetaSerializable> T deserializeObject(Class<T> clazz, Meta meta) {
        String className = clazz.getName();

        boolean c1 = false;
        try {
            try {
                Method m = clazz.getMethod("deserialize", Meta.class);
                return (T) m.invoke(null, meta);
            } catch (NoSuchMethodException ignored) {
            } catch (NullPointerException e) {
                c1 = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            Constructor<T> constructor = clazz.getDeclaredConstructor(Meta.class);
            return constructor.newInstance(meta);
        } catch (Exception e) {
            if (!c1)
                throw new ObjectDeserializationException("ERROR: Could NOT find a deserialization method for " + className, e, ObjectDeserializationException.DeserializationExceptionCause.NO_DESERIALIZATION_METHOD);
            throw new ObjectDeserializationException("ERROR: Deserialization method non-static for " + className, e, ObjectDeserializationException.DeserializationExceptionCause.NO_DESERIALIZATION_METHOD);
        }
    }

    Meta serialize();

    default void serialize(GravSerializer serializer) {
        serialize().serialize(serializer);
    }
}
