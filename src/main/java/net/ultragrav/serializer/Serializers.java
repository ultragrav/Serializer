package net.ultragrav.serializer;

import java.util.*;

public class Serializers {
    private static final List<SerializerElement> SERIALIZERS = new ArrayList<>();

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
                Map<Object, Object> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) deserializeObject(serializer);
                    if (entry == null) {
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
                    l.add(deserializeObject(serializer));
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
        throw new IllegalArgumentException("Cannot serialize object of type: " + obj.getClass().getName());
    }

    public static Object deserializeObject(GravSerializer serializer, Object... args) {
        byte type = serializer.readByte();
        if (type == 0) {
            return null;
        }
        if (type > SERIALIZERS.size()) {
            throw new IllegalArgumentException("Invalid object type: " + (type-1));
        }
        return SERIALIZERS.get(type - 1).getSerializer().deserialize(serializer, args);
    }
}
