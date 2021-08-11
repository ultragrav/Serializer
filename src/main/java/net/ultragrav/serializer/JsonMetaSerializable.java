package net.ultragrav.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Requires either a static method deserialize(JsonMeta) OR a constructor(JsonMeta)
 */
public interface JsonMetaSerializable extends GravSerializable {
    static Object deserializeObject(Class<? extends JsonMetaSerializable> clazz, GravSerializer serializer) {
        JsonMeta meta = JsonMeta.deserialize(serializer);

        String className = clazz.getName();

        boolean c1 = false;
        try {
            try {
                Method m = clazz.getMethod("deserialize", JsonMeta.class);
                return m.invoke(null, meta);
            } catch (NoSuchMethodException ignored) {
            } catch (NullPointerException e) {
                c1 = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            Constructor<?> constructor = clazz.getConstructor(JsonMeta.class);
            return constructor.newInstance(meta);
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
