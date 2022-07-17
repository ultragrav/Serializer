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
