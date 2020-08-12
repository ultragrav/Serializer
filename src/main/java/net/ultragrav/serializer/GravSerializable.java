package net.ultragrav.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

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

        serializer = serializer.readSerializer(); //Read the buffer

        Class<?>[] argumentTypes = new Class<?>[otherArguments.length + 1];
        Object[] arguments = new Object[otherArguments.length + 1];
        argumentTypes[0] = net.ultragrav.serializer.GravSerializer.class;
        arguments[0] = serializer;
        for (int i = 0; i < otherArguments.length; i++) {
            argumentTypes[i + 1] = otherArguments[i].getClass();
            arguments[i + 1] = otherArguments[i];
        }
        boolean c1 = false;
        try {
            Class<?> clazz = Class.forName(className);
            try {
                Method m = clazz.getMethod("deserialize", argumentTypes);
                return m.invoke(null, arguments);
            } catch (NoSuchMethodException ignored) {
            } catch (NullPointerException e) {
                c1 = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            Constructor<?> constructor = clazz.getConstructor(argumentTypes);
            return constructor.newInstance(arguments);
        } catch (ClassNotFoundException e) {
            System.out.println("COULD NOT FIND CLASS " + className);
            return null;
        } catch (Exception e) {
            if (!c1)
                throw new ObjectDeserializationException("ERROR: Could NOT find a deserialization method for " + className, 0);
            throw new ObjectDeserializationException("ERROR: Deserialization method non-static for " + className, 0);
        }
    }

    static void serializeObject(net.ultragrav.serializer.GravSerializer serializer, GravSerializable serializable) {
        serializer.writeString(serializable.getClass().getName());
        GravSerializer serializer1 = new GravSerializer(); //Buffer the object - This is for the reason of if a object's class could not be found on deserialization
        //Then it should be able to skip the object
        serializable.serialize(serializer1); //Serialize
        serializer.writeSerializer(serializer1); //Write the buffer
    }

    void serialize(net.ultragrav.serializer.GravSerializer serializer);
}
