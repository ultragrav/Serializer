package net.ultragrav.serializer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class JsonMetaUpdateRecord {
    protected Set<String> updatedFields = new HashSet<>();
    public Set<JsonMetaUpdateRecord> children = new HashSet<>();
    public List<Consumer<String>> listeners = new ArrayList<>();

    public boolean markDirty(String key) {
        listeners.forEach(c -> c.accept(key));
        return updatedFields.add(key);
    }

    public List<String> getUpdatedFields() {
        return new ArrayList<>(updatedFields);
    }

    public void clear() {
        this.updatedFields.clear();
        children.forEach(JsonMetaUpdateRecord::clear);
    }
}
