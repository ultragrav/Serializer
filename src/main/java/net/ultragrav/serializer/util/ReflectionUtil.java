package net.ultragrav.serializer.util;

import net.ultragrav.serializer.JsonMeta;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class ReflectionUtil {
    /**
     * Get the first method compatible with the provided parameters
     * Note: Only gets public methods
     *
     * @param clazz  Class
     * @param params Parameters
     * @return Method
     */
    public static Method getCompatibleMethod(Class<?> clazz, String name, Object... params) {
        Method[] methods = clazz.getMethods();
        Label1:
        for (Method method : methods) {
            if (!method.getName().equals(name)) continue;

            Class<?>[] mp = method.getParameterTypes();
            if (mp.length != params.length) continue;

            for (int i = 0; i < mp.length; i++) {
                if (mp[i].isPrimitive()) {
                    Class<?> unboxed = mp[i];
                    Class<?> boxed = MethodType.methodType(unboxed).wrap().returnType();
                    if (!boxed.isInstance(params[i]) && !unboxed.isInstance(params[i])) continue Label1;
                } else {
                    if (!mp[i].isInstance(params[i])) continue Label1;
                }
            }

            return method;
        }
        return null;
    }

    /**
     * Get the first constructor compatible with the provided parameters
     * Note: Only gets public constructors
     *
     * @param clazz  Class
     * @param params Parameters
     * @param <T>    Type of Class
     * @return Constructor
     */
    public static <T> Constructor<T> getCompatibleConstructor(Class<T> clazz, Object... params) {
        Constructor<?>[] constructors = clazz.getConstructors();
        Label1:
        for (Constructor<?> constructor : constructors) {
            Class<?>[] mp = constructor.getParameterTypes();
            if (mp.length != params.length) continue;

            for (int i = 0; i < mp.length; i++) {
                if (mp[i].isPrimitive()) {
                    Class<?> unboxed = mp[i];
                    Class<?> boxed = MethodType.methodType(unboxed).wrap().returnType();
                    if (!boxed.isInstance(params[i]) && !unboxed.isInstance(params[i])) continue Label1;
                } else {
                    if (!mp[i].isInstance(params[i])) continue Label1;
                }
            }

            return (Constructor<T>) constructor;
        }
        return null;
    }

}
