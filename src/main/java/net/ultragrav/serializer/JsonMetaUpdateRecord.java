package net.ultragrav.serializer;

import java.util.HashSet;
import java.util.Set;

public class JsonMetaUpdateRecord {
    public Set<String> updatedFields = new HashSet<>();
    public Set<JsonMetaUpdateRecord> children = new HashSet<>();

    public void clear() {
        this.updatedFields.clear();
        children.forEach(JsonMetaUpdateRecord::clear);
    }
}
