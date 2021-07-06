package net.ultragrav.serializer;

import lombok.val;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

//TODO make thread-safe
public class JsonMeta implements GravSerializable {

    private final Map<String, Object> data = new HashMap<>();

    private String delimiter = "\\.";
    private String[] path = new String[0];
    private JsonMeta parent = null;

    private ReentrantLock lock = new ReentrantLock();

    private final JsonMetaUpdateRecord record = new JsonMetaUpdateRecord();

    public JsonMeta() {
    }

    public JsonMeta(String delimiter) {
        this.delimiter = delimiter;
    }

    public JsonMetaUpdateRecord getRecord() {
        return record;
    }

    public Set<String> getKeys() {
        return this.data.keySet();
    }

    public <T> T get(String path) {
        return get(path.split(delimiter));
    }

    public <T> T get(String... path) {
        lock.lock();
        try {
            JsonMeta current = this;
            for (int i = 0, pathLength = path.length; i < pathLength; i++) {
                String s = path[i];
                Object o = current.data.get(s);
                if (o instanceof JsonMeta) {
                    current = (JsonMeta) o;
                } else {
                    if (i != path.length - 1)
                        return null;
                    return (T) o;
                }
            }
            return (T) current;
        } finally {
            lock.unlock();
        }
    }

    public <T> T getOrSet(String path, T defaultValue) {
        lock.lock();
        try {
            T obj = get(path);
            if (obj == null)
                set(path, defaultValue);
            return defaultValue;
        } finally {
            lock.unlock();
        }
    }

    public void set(String path, Object value) {
        set(path.split(delimiter), value);
    }

    public void set(String[] path, Object value) { //Doesn't use recursion, may change later
        lock.lock();
        try {
            JsonMeta current = this;
            for (int i = 0, pathLength = path.length; i < pathLength; i++) {
                String s = path[i];
                Object o = current.data.get(s);

                if (i == pathLength - 1) {

                    current.data.put(s, value);
                    current.markDirty(s);

                    //Link it if it's a JsonMeta
                    if (value instanceof JsonMeta) {
                        JsonMeta v = (JsonMeta) value;
                        link(current, v, s);
                    }

                    return;
                }

                if (!(o instanceof JsonMeta)) {
                    //Create new json meta at specific path
                    JsonMeta next = new JsonMeta(delimiter);

                    link(current, next, path[i]);

                    //Put the next JsonMeta
                    current.data.put(s, next);
                    current = next;
                } else {
                    current = (JsonMeta) o;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param valName The last entry in the path that the child should inherit eg. adding meta A to B at apple.dog the last entry would be dog
     */
    private static void link(JsonMeta parent, JsonMeta child, String valName) {

        //Compute current path
        String[] currentPath = Arrays.copyOf(parent.path, parent.path.length+1);
        currentPath[currentPath.length-1] = valName;

        //Set paths
        child.path = currentPath;
        child.parent = parent;

        //Set lock
        child.lock = parent.lock;

        //Set record's children
        parent.record.children.add(child.record);

    }

    private void markDirty(String key) {
        record.updatedFields.add(key);
        if (parent != null) {
            if(path.length == 0) throw new IllegalStateException("This shouldn't happen.");
            parent.markDirty(path[path.length - 1]);
        }
    }

    public JsonMeta getRoot() {
        return parent == null ? this : parent.getRoot();
    }

    /**
     * Literally just get, but casted to JsonMeta immediately
     */
    public JsonMeta getAsJsonMeta(String path) {
        return get(path);
    }

    @Override
    public String toString() {
        return recursiveToString(0);
    }

    private String recursiveToString(int indentation) {

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentation; i++) {
            indent.append("  ");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{").append("\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object val = entry.getValue();
            String valStr = val instanceof JsonMeta ? ((JsonMeta) val).recursiveToString(indentation+1) : val.toString();
            builder.append(indent).append("  ").append(entry.getKey()).append(": ").append(valStr).append("\n");
        }

        return builder.append(indent).append("}").toString();
    }

    public JsonMeta reduce() {
        lock.lock();
        try {
            JsonMeta meta = new JsonMeta();

            //Iterate through updated fields
            for (String updatedField : record.updatedFields) {
                Object val = get(updatedField);

                //Check if it's a JsonMeta with it's parent as us
                if (val instanceof JsonMeta && ((JsonMeta) val).parent == this) {
                    meta.data.put(updatedField, ((JsonMeta) val).reduce());
                } else {
                    meta.data.put(updatedField, val);
                }
            }
            return meta;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void serialize(GravSerializer serializer) {
        serialize(serializer, false);
    }

    public void serialize(GravSerializer serializer, boolean reduced) {
        int len = reduced ? record.updatedFields.size() : this.data.size();

        serializer.writeBoolean(reduced); //Probably not needed but who cares about an extra couple bytes
        serializer.writeInt(len);

        if (reduced) {
            for (String updatedField : record.updatedFields) {
                Object val = get(updatedField);
                serializeObject(serializer, updatedField, val, true);
            }
        } else {
            for (Map.Entry<String, Object> entry : this.data.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                serializeObject(serializer, key, val, false);
            }
        }
    }

    private void serializeObject(GravSerializer serializer, String key, Object val, boolean reduced) {
        if (val instanceof JsonMeta && ((JsonMeta) val).parent == this) { //If it's parent isn't us, then treat it like any other object
            serializer.writeByte((byte) 0);
            serializer.writeString(key);
            ((JsonMeta) val).serialize(serializer, reduced);
        } else {
            serializer.writeByte((byte) 1);
            serializer.writeString(key);
            serializer.writeObject(val);
        }
    }

    public static JsonMeta deserialize(GravSerializer serializer) {

        JsonMeta meta = new JsonMeta();

        boolean reduced = serializer.readBoolean();
        int len = serializer.readInt();

        for (int i = 0; i < len; i++) {
            byte type = serializer.readByte();
            String key = serializer.readString();
            Object object;
            if(type == 0) {
                object = JsonMeta.deserialize(serializer);
            } else {
                object = serializer.readObject();
            }
            meta.data.put(key, object);
        }

        return meta;
    }

    public static void main(String[] args) {
        JsonMeta meta = new JsonMeta();
        meta.set("hey.one.two.three.num", 1);
        meta.set("hey.one.two.four.num", 2);
        System.out.println(meta);

        meta.getRecord().clear();

        meta.set("hey.one.two.three.num", 3);
        meta = meta.reduce();
        System.out.println(meta);
    }
}
