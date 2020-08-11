package net.ultragrav.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface GravSerializable {
    Map<String, String> relocationMappings = new HashMap<>();

    static Object deserializeObject(net.ultragrav.serializer.GravSerializer serializer, Object... otherArguments) {

        String className = serializer.readString();
        // Do exact matches first
        for (Map.Entry<String, String> mapping : relocationMappings.entrySet()) {
            if (className.equals(mapping.getKey())) {
                className = mapping.getValue();
            }
        }
        // Then packages, etc.
        for (Map.Entry<String, String> mapping : relocationMappings.entrySet()) {
            className = className.replace(Pattern.quote(mapping.getKey()), Matcher.quoteReplacement(mapping.getValue()));
        }

        Class<?>[] argumentTypes = new Class<?>[otherArguments.length + 1];
        Object[] arguments = new Object[otherArguments.length + 1];
        argumentTypes[0] = net.ultragrav.serializer.GravSerializer.class;
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

    static void serializeObject(net.ultragrav.serializer.GravSerializer serializer, GravSerializable serializable) {
        serializer.writeString(serializable.getClass().getName());
        serializable.serialize(serializer);
    }

    void serialize(net.ultragrav.serializer.GravSerializer serializer);
}
