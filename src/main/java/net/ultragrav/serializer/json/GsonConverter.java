package net.ultragrav.serializer.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.ultragrav.serializer.JsonMeta;

import java.util.ArrayList;
import java.util.List;

public class GsonConverter {
    public static JsonMeta fromGson(JsonObject object) {
        return (JsonMeta) fromGsonInternal(object);
    }

    public static JsonObject toGson(JsonMeta meta) {
        throw new RuntimeException("Not yet implemented");
    }

    private static Object fromGsonInternal(JsonElement element) {
        if (element.isJsonNull()) return null;
        if (element.isJsonObject()) {
            JsonObject jsObj = (JsonObject) element;

            JsonMeta ret = new JsonMeta();
            for (String str : jsObj.keySet()) {
                ret.set(str, fromGsonInternal(jsObj.get(str)));
            }
            return ret;
        }
        if (element.isJsonArray()) {
            JsonArray arr = (JsonArray) element;

            List<Object> list = new ArrayList<>();
            arr.forEach(el -> list.add(fromGsonInternal(el)));
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = (JsonPrimitive) element;
            if (primitive.isNumber()) {
                try {
                    return primitive.getAsInt();
                } catch(Exception ignored) {}
                try {
                    return primitive.getAsDouble();
                } catch(Exception ignored) {}
                throw new UnsupportedOperationException("Tried to convert number that is not int or double");
            }
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isString()) {
                return primitive.getAsString();
            }
        }
        throw new UnsupportedOperationException("Tried to convert invalid JsonElement");
    }
}
