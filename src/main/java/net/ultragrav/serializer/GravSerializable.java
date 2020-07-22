package net.ultragrav.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface GravSerializable {
    static Object deserializeObject(GravSerializer serializer, Object... otherArguments) {
        String className = serializer.readString();
        Class<?>[] argumentTypes = new Class<?>[otherArguments.length + 1];
        Object[] arguments = new Object[otherArguments.length + 1];
        argumentTypes[0] = GravSerializer.class;
        arguments[0] = serializer;
        for (int i = 0; i < otherArguments.length; i++) {
            argumentTypes[i + 1] = otherArguments[i].getClass();
            arguments[i + 1] = otherArguments[i];
        }
        try {
            Class<?> clazz = Class.forName(className);
            try {
                Method m = clazz.getMethod("deserialize", argumentTypes);
                return m.invoke(null, arguments);
            } catch (Exception ignored) {
            }
            Constructor<?> constructor = clazz.getConstructor(argumentTypes);
            return constructor.newInstance(arguments);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void serializeObject(GravSerializer serializer, GravSerializable serializable) {
        serializer.writeString(serializable.getClass().getName());
        serializable.serialize(serializer);
    }

    void serialize(GravSerializer serializer);
}
