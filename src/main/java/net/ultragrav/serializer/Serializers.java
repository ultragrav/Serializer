package net.ultragrav.serializer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

public class Serializers {
    private static final List<SerializerElement> SERIALIZERS = new ArrayList<>();

    private static final Map<Class<?>, Serializer<?>> CUSTOM_SERIALIZERS = new HashMap<>();

    static {
        //0
        SERIALIZERS.add(new SerializerElement(String.class, new Serializer<String>() {
            @Override
            public void serialize(GravSerializer serializer, Object s) {
                serializer.writeString((String) s);
            }

            @Override
            public String deserialize(GravSerializer serializer, Object... args) {
                return serializer.readString();
            }
        }));
        //1
        SERIALIZERS.add(new SerializerElement(byte[].class, new Serializer<byte[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeByteArray((byte[]) t);
            }

            @Override
            public byte[] deserialize(GravSerializer serializer, Object... args) {
                return serializer.readByteArray();
            }
        }));
        //2
        SERIALIZERS.add(new SerializerElement(Byte.class, new Serializer<Byte>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeByte((Byte) t);
            }

            @Override
            public Byte deserialize(GravSerializer serializer, Object... args) {
                return serializer.readByte();
            }
        }));
        //3
        SERIALIZERS.add(new SerializerElement(Double.class, new Serializer<Double>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeDouble((Double) t);
            }

            @Override
            public Double deserialize(GravSerializer serializer, Object... args) {
                return serializer.readDouble();
            }
        }));
        //4
        SERIALIZERS.add(new SerializerElement(Integer.class, new Serializer<Integer>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeInt((Integer) t);
            }

            @Override
            public Integer deserialize(GravSerializer serializer, Object... args) {
                return serializer.readInt();
            }
        }));
        //5
        SERIALIZERS.add(new SerializerElement(Long.class, new Serializer<Long>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeLong((Long) t);
            }

            @Override
            public Long deserialize(GravSerializer serializer, Object... args) {
                return serializer.readLong();
            }
        }));
        //6
        SERIALIZERS.add(new SerializerElement(UUID.class, new Serializer<UUID>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeUUID((UUID) t);
            }

            @Override
            public UUID deserialize(GravSerializer serializer, Object... args) {
                return serializer.readUUID();
            }
        }));
        //7
        SERIALIZERS.add(new SerializerElement(Map.class, new Serializer<Map<?, ?>>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                Map<?, ?> map = (Map<?, ?>) t;
                serializer.writeInt(map.size());
                for (Object o : map.entrySet()) {
                    serializeObject(serializer, o);
                }
            }

            @Override
            public Map<?, ?> deserialize(GravSerializer serializer, Object... args) {
                int size = serializer.readInt();
                Map<Object, Object> map = new ConcurrentHashMap<>();
                for (int i = 0; i < size; i++) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) deserializeObject(serializer);
                    if (entry == null || entry.getValue() == null) {
                        continue;
                    }
                    map.put(entry.getKey(), entry.getValue());
                }
                return map;
            }
        }));
        //8
        SERIALIZERS.add(new SerializerElement(Map.Entry.class, new Serializer<Map.Entry<?, ?>>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                Map.Entry<?, ?> en = (Map.Entry<?, ?>) t;
                serializeObject(serializer, en.getKey());
                serializeObject(serializer, en.getValue());
            }

            @Override
            public Map.Entry<?, ?> deserialize(GravSerializer serializer, Object... args) {
                Object key = deserializeObject(serializer);
                Object value = deserializeObject(serializer);
                return new AbstractMap.SimpleEntry<>(key, value);
            }
        }));
        //9
        SERIALIZERS.add(new SerializerElement(List.class, new Serializer<List<?>>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                List<?> l = (List<?>) t;
                serializer.writeInt(l.size());
                for (Object o : l) {
                    serializeObject(serializer, o);
                }
            }

            @Override
            public List<?> deserialize(GravSerializer serializer, Object... args) {
                int size = serializer.readInt();
                List<Object> l = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    l.add(deserializeObject(serializer, args));
                }
                return l;
            }
        }));
        //10
        SERIALIZERS.add(new SerializerElement(Enum.class, new Serializer<Enum<?>>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                Enum<?> e = (Enum<?>) t;
                serializer.writeString(t.getClass().getName());
                serializer.writeInt(e.ordinal());
            }

            @Override
            public Enum<?> deserialize(GravSerializer serializer, Object... args) {
                try {
                    String className = serializer.readString();
                    int ordinal = serializer.readInt();
                    Class<?> clazz = Class.forName(className);
                    if (!clazz.isEnum()) {
                        return null;
                    }
                    return (Enum<?>) clazz.getEnumConstants()[ordinal];
                } catch (Exception e) {
                    System.out.println("Error while deserializing enum:");
                    e.printStackTrace();
                }
                return null;
            }
        }));
        //11
        SERIALIZERS.add(new SerializerElement(GravSerializable.class, new Serializer<GravSerializable>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                GravSerializable.serializeObject(serializer, (GravSerializable) t);
            }

            @Override
            public GravSerializable deserialize(GravSerializer serializer, Object... args) {
                return (GravSerializable) GravSerializable.deserializeObject(serializer, args);
            }
        }));
        //12
        SERIALIZERS.add(new SerializerElement(int[].class, new Serializer<int[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                int[] arr = (int[]) t;
                serializer.writeInt(arr.length);
                for (int i : arr) {
                    serializer.writeInt(i);
                }
            }

            @Override
            public int[] deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                int[] ret = new int[len];
                for (int i = 0; i < len; i++) {
                    ret[i] = serializer.readInt();
                }
                return ret;
            }
        }));
        //13
        SERIALIZERS.add(new SerializerElement(Object[].class, new Serializer<Object[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                Object[] arr = (Object[]) t;
                serializer.writeInt(arr.length);
                for (Object obj : arr) {
                    serializeObject(serializer, obj);
                }
            }

            @Override
            public Object[] deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                Object[] ret = new Object[len];
                for (int i = 0; i < len; i++) {
                    ret[i] = serializer.readObject(args);
                }
                return ret;
            }
        }));
        //14
        SERIALIZERS.add(new SerializerElement(Boolean.class, new Serializer<Boolean>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeBoolean((Boolean) t);
            }

            @Override
            public Boolean deserialize(GravSerializer serializer, Object... args) {
                return serializer.readBoolean();
            }
        }));
        // 15
        SERIALIZERS.add(new SerializerElement(boolean[].class, new Serializer<boolean[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeBooleanArray((boolean[]) t);
            }

            @Override
            public boolean[] deserialize(GravSerializer serializer, Object... args) {
                return serializer.readBooleanArray();
            }
        }));
        // 16
        SERIALIZERS.add(new SerializerElement(long[].class, new Serializer<long[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                long[] arr = (long[]) t;
                serializer.writeInt(arr.length);
                for (long l : arr) {
                    serializer.writeLong(l);
                }
            }

            @Override
            public long[] deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = serializer.readLong();
                }
                return arr;
            }
        }));
        // 17
        SERIALIZERS.add(new SerializerElement(short[].class, new Serializer<short[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                short[] arr = (short[]) t;
                serializer.writeInt(arr.length);
                for (short s : arr) {
                    serializer.writeShort(s);
                }
            }

            @Override
            public short[] deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                short[] arr = new short[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = serializer.readShort();
                }
                return arr;
            }
        }));
        // 18
        SERIALIZERS.add(new SerializerElement(Short.class, new Serializer<Short>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeShort((short) t);
            }

            @Override
            public Short deserialize(GravSerializer serializer, Object... args) {
                return serializer.readShort();
            }
        }));
        // 19
        SERIALIZERS.add(new SerializerElement(Float.class, new Serializer<Float>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeFloat((float) t);
            }

            @Override
            public Float deserialize(GravSerializer serializer, Object... args) {
                return serializer.readFloat();
            }
        }));
        // 20
        SERIALIZERS.add(new SerializerElement(float[].class, new Serializer<float[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                float[] arr = (float[]) t;
                serializer.writeInt(arr.length);
                for (float f : arr) {
                    serializer.writeFloat(f);
                }
            }

            @Override
            public float[] deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                float[] arr = new float[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = serializer.readFloat();
                }
                return arr;
            }
        }));
        // 21
        SERIALIZERS.add(new SerializerElement(double[].class, new Serializer<double[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                double[] arr = (double[]) t;
                serializer.writeInt(arr.length);
                for (double d : arr) {
                    serializer.writeDouble(d);
                }
            }

            @Override
            public double[] deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                double[] arr = new double[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = serializer.readDouble();
                }
                return arr;
            }
        }));
        // 22
        SERIALIZERS.add(new SerializerElement(Character.class, new Serializer<Character>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeChar((char) t);
            }

            @Override
            public Character deserialize(GravSerializer serializer, Object... args) {
                return serializer.readChar();
            }
        }));
        // 23
        SERIALIZERS.add(new SerializerElement(char[].class, new Serializer<char[]>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                char[] arr = (char[]) t;
                serializer.writeInt(arr.length);
                for (char c : arr) {
                    serializer.writeChar(c);
                }
            }

            @Override
            public char[] deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                char[] arr = new char[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = serializer.readChar();
                }
                return arr;
            }
        }));
        // 24
        SERIALIZERS.add(new SerializerElement(AtomicBoolean.class, new Serializer<AtomicBoolean>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeBoolean(((AtomicBoolean) t).get());
            }

            @Override
            public AtomicBoolean deserialize(GravSerializer serializer, Object... args) {
                return new AtomicBoolean(serializer.readBoolean());
            }
        }));
        // 25
        SERIALIZERS.add(new SerializerElement(AtomicInteger.class, new Serializer<AtomicInteger>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeInt(((AtomicInteger) t).get());
            }

            @Override
            public AtomicInteger deserialize(GravSerializer serializer, Object... args) {
                return new AtomicInteger(serializer.readInt());
            }
        }));
        // 26
        SERIALIZERS.add(new SerializerElement(AtomicIntegerArray.class, new Serializer<AtomicIntegerArray>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                AtomicIntegerArray arr = (AtomicIntegerArray) t;
                int len = arr.length();
                serializer.writeInt(len);
                for (int i = 0; i < len; i++) {
                    serializer.writeInt(arr.get(i));
                }
            }

            @Override
            public AtomicIntegerArray deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                AtomicIntegerArray ret = new AtomicIntegerArray(len);
                for (int i = 0; i < len; i++) {
                    ret.set(i, serializer.readInt());
                }
                return ret;
            }
        }));
        // 27
        SERIALIZERS.add(new SerializerElement(AtomicLong.class, new Serializer<AtomicLong>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeLong(((AtomicLong) t).get());
            }

            @Override
            public AtomicLong deserialize(GravSerializer serializer, Object... args) {
                return new AtomicLong(serializer.readLong());
            }
        }));
        // 28
        SERIALIZERS.add(new SerializerElement(AtomicLongArray.class, new Serializer<AtomicLongArray>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                AtomicLongArray arr = (AtomicLongArray) t;
                int len = arr.length();
                serializer.writeInt(len);
                for (int i = 0; i < len; i ++) {
                    serializer.writeLong(arr.get(i));
                }
            }

            @Override
            public AtomicLongArray deserialize(GravSerializer serializer, Object... args) {
                int len = serializer.readInt();
                AtomicLongArray ret = new AtomicLongArray(len);
                for (int i = 0; i < len; i++) {
                    ret.set(i, serializer.readLong());
                }
                return ret;
            }
        }));
        // 29
        SERIALIZERS.add(new SerializerElement(AtomicReference.class, new Serializer<AtomicReference<?>>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                serializer.writeObject(((AtomicReference<?>) t).get());
            }

            @Override
            public AtomicReference<?> deserialize(GravSerializer serializer, Object... args) {
                return new AtomicReference<>(serializer.readObject());
            }
        }));
        // 30
        SERIALIZERS.add(new SerializerElement(Set.class, new Serializer<Set<?>>() {
            @Override
            public void serialize(GravSerializer serializer, Object t) {
                Set<?> set = (Set<?>) t;
                serializer.writeInt(set.size());
                for (Object o : set) {
                    serializeObject(serializer, o);
                }
            }

            @Override
            public Set<?> deserialize(GravSerializer serializer, Object... args) {
                Set<Object> ret = new HashSet<>();
                int len = serializer.readInt();
                for (int i = 0; i < len; i++) {
                    ret.add(deserializeObject(serializer, args));
                }
                return ret;
            }
        }));
    }

    public static boolean canSerialize(Class<?> clazz) {
        for (int i = 0; i < SERIALIZERS.size(); i++) {
            SerializerElement s = SERIALIZERS.get(i);
            if (s.getClazz().isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static void serializeObject(GravSerializer serializer, Object obj) {
        if (obj == null) {
            serializer.writeByte((byte) 0);
            return;
        }
        for (int i = 0; i < SERIALIZERS.size(); i++) {
            SerializerElement s = SERIALIZERS.get(i);
            if (s.getClazz().isInstance(obj)) {
                serializer.writeByte((byte) (i + 1));
                s.getSerializer().serialize(serializer, obj);
                return;
            }
        }
        for (Class<?> clazz : CUSTOM_SERIALIZERS.keySet()) {
            if (clazz.isInstance(obj)) {
                serializer.writeByte((byte) 0xFF);
                serializer.writeString(clazz.getName());
                CUSTOM_SERIALIZERS.get(clazz).serialize(serializer, obj);
                return;
            }
        }
        throw new IllegalArgumentException("Cannot serialize object of type: " + obj.getClass().getName());
    }

    public static Object deserializeObject(GravSerializer serializer, Object... args) {
        int type = serializer.readByte() & 0xFF; // Ensure unsigned byte
        if (type == 0) {
            return null;
        }
        if (type == 0xFF) {
            Class<?> clazz;
            try {
                clazz = Class.forName(serializer.readString());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot deserialize object due to missing Custom Serializer class");
            }

            Serializer<?> ser = CUSTOM_SERIALIZERS.get(clazz);
            if (ser == null) {
                throw new IllegalArgumentException("Cannot deserialize object due to missing Serializer (Custom Serializer)");
            }
            return ser.deserialize(serializer, args);
        }
        if (type > SERIALIZERS.size()) {
            throw new IllegalArgumentException("No serializer found! Invalid object type: " + (type - 1));
        }
        return SERIALIZERS.get(type - 1).getSerializer().deserialize(serializer, args);
    }

    /**
     * Register a custom serializer
     * Use this for any API classes that you cannot edit
     * Allows object to be serialized and deserialized
     * after this call.
     * <p>
     * Note: Ensure this call happens before
     * any deserialization of the object
     *
     * @param clazz      Class
     * @param serializer Serializer
     */
    public static void registerSerializer(Class<?> clazz, Serializer<?> serializer) {
        CUSTOM_SERIALIZERS.put(clazz, serializer);
    }
}
