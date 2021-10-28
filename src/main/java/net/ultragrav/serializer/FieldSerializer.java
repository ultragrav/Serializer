package net.ultragrav.serializer;

import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Serialize the fields of a class without requiring it to be GravSerializable.
 * <p>
 * Note: This operation is not always safe
 */
@SuppressWarnings("unchecked")
public class FieldSerializer {
    private static Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Serialize the fields of an object into a meta
     * <p>
     * If recursive is true, serialize the field of nonserializable objects
     *
     * @param obj       Object
     * @param recursive Recursive serialization
     * @return Meta containing the fields of obj
     */
    public static Meta serializeFields(Object obj, boolean recursive) {
        List<Field> allFields = new ArrayList<>();
        Class<?> cl = obj.getClass();
        while (cl != null) {
            allFields.addAll(Arrays.asList(cl.getDeclaredFields()));
            cl = cl.getSuperclass();
        }

        Meta ret = new Meta();
        for (Field f : allFields) {
            // Don't serialize special (jvm) fields.
            if (f.getName().contains("$")) continue;

            // Ignore fields with @NoSerialize
            if (f.getAnnotation(NoSerialize.class) != null) continue;

            f.setAccessible(true);
            try {
                if (!Serializers.canSerialize(f.getType()) && recursive) {
                    Meta m = new Meta();
                    m.set("__class", f.getType().getName());
                    m.setAll(serializeFields(f.get(obj), true));
                    ret.set(f.getName(), m);
                } else {
                    ret.set(f.getName(), f.get(obj));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * Load all fields from a meta into the object
     * <p>
     * Note: This will only override fields in the object
     * that are present in the meta.
     * <p>
     * This does not consider the {@link NoSerialize} annotation,
     * even fields annotated with it can be modified if they are
     * present in the meta.
     *
     * @param t    Object
     * @param meta Meta
     * @param <T>  Type of object
     */
    public static <T> void deserializeFields(T t, Meta meta) {
        try {
            Map<String, Object> m = meta.asMap();
            for (Map.Entry<String, Object> ent : m.entrySet()) {
                Field f = null;
                Class<?> cl = t.getClass();
                while (f == null && cl != null) {
                    f = cl.getDeclaredField(ent.getKey());
                    cl = cl.getSuperclass();
                }
                if (f == null) {
                    System.out.println("Warning: No field found for " + ent.getKey() + ", ignoring it.");
                    continue;
                }
                f.setAccessible(true);
                if (f.getType() == Meta.class) {
                    Map<String, Object> map = (Map<String, Object>) ent.getValue();
                    f.set(t, new Meta(map));
                } else if (!Serializers.canSerialize(f.getType()) && ent.getValue() instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) ent.getValue();
                    if (map.containsKey("__class")) {
                        String cla = (String) map.remove("__class");
                        Class<?> clazz = Class.forName(cla);

                        // Warning if the classes don't match
                        if (clazz != f.getType()) {
                            System.out.println("Warning: Could not deserialize field: " + f.getName() + " because of type mismatch!");
                            continue;
                        }

                        // Deserialize into existing object if present
                        Object obj = f.get(t);

                        // If no object is present create it
                        if (obj == null) {
                            try {
                                // Search for empty constructor
                                Constructor<?> con = clazz.getConstructor();
                                obj = con.newInstance();
                            } catch (NoSuchMethodException err) {
                                // No empty constructor found, so we just allocate
                                // a new instance and hope it is fine.
                                obj = unsafe.allocateInstance(clazz);
                            }
                        }

                        Meta m2 = new Meta(map);
                        deserializeFields(obj, m2);
                        f.set(t, obj);
                    }
                } else {
                    f.set(t, ent.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        printBits(-12);
    }

    public static void printBits(long num) {
        for (int i = 63; i >= 0; i--) {
            System.out.print(num >>> i & 1);
        }
    }

}
