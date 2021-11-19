package net.ultragrav.serializer;

import org.junit.jupiter.api.Test;

import java.util.Objects;

public class TestJsonMeta {
    @Test
    public void testGet() {
        JsonMeta test = new JsonMeta();
        test.set("one", 1);
        test.set("whynot", "It doesn't really matter what I put here");

        assert Objects.equals(test.get("one"), 1);
        assert Objects.equals(test.get("whynot"), "It doesn't really matter what I put here");
    }

    @Test
    public void testNested() {
        JsonMeta parent = new JsonMeta();
        JsonMeta child = new JsonMeta();
        child.set("one", 1);
        child.set("two", "2");

        parent.set("child1", child);
        parent.set("child2.nested", "Nested test");

        System.out.println(parent.<Integer>get("child1.one"));

        assert Objects.equals(parent.get("child1.one"), 1);
        assert Objects.equals(parent.get("child1.two"), "2");
        assert Objects.equals(parent.get("child2.nested"), "Nested test");
    }
}
