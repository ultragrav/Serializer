package net.ultragrav.serializer;

import net.ultragrav.serializer.util.JsonUtil;
import org.junit.jupiter.api.Test;

public class TestJsonParsing {
    @Test
    public void testJsonParsing() {
        String json = "{\"one\":1,\"two\":\"2\",\"three\":true,\"four\":false,\"five\":null}";
        JsonMeta meta = JsonUtil.readJson(json);
        assert meta.get("one") == Integer.valueOf(1);
        assert "2".equals(meta.get("two"));
        assert meta.get("three") == Boolean.TRUE;
        assert meta.get("four") == Boolean.FALSE;
        assert meta.get("five") == null;
    }

    @Test
    public void testJsonConsistence() {
        // Make an object, convert it to json, then back
        // to an object and make sure it's the same

        JsonMeta meta = new JsonMeta();
        meta.set("one", 1);
        meta.set("two", "2");
        meta.set("three", true);
        meta.set("four", false);
        meta.set("five", null);
        meta.set("six", "This is a multi-line string\nwith a newline in it");

        JsonMeta meta2 = JsonUtil.readJson(meta.toJson());

        assert meta2.get("one") == Integer.valueOf(1);
        assert "2".equals(meta2.get("two"));
        assert meta2.get("three") == Boolean.TRUE;
        assert meta2.get("four") == Boolean.FALSE;
        assert meta2.get("five") == null;
        assert "This is a multi-line string\nwith a newline in it".equals(meta2.get("six"));

    }

    @Test
    public void testMultiLineString() {
        String json = "{\"text\":\"\\n\\nYes\"}";
        JsonMeta meta = JsonUtil.readJson(json);
        assert meta.get("text").equals("\n\nYes");
    }

    @Test
    public void testJsonComment() {
        String json = "{\"one\":1,\"two\":\"2\",\"three\":true,/* This is a multiline \n comment */\"four\":false,\"five\":null}// This is a comment\n";
        JsonMeta meta = JsonUtil.readJson(json);
        assert meta.get("one") == Integer.valueOf(1);
        assert "2".equals(meta.get("two"));
        assert meta.get("three") == Boolean.TRUE;
        assert meta.get("four") == Boolean.FALSE;
        assert meta.get("five") == null;
    }
}
