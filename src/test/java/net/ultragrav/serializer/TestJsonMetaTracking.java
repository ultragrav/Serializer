package net.ultragrav.serializer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestJsonMetaTracking {
    @Test
    public void testSimple() {
        JsonMeta test = new JsonMeta(true);
        test.set("one", 1);
        test.set("whynot", "It doesn't really matter what I put here");

        assert Objects.equals(test.reduce().get("one"), test.get("one"));
        assert Objects.equals(test.reduce().get("whynot"), test.get("whynot"));
    }

    @Test
    public void testNested() {
        JsonMeta test = new JsonMeta(true);

        JsonMeta other = test.getOrSet("test1.test2", new JsonMeta());

        assert other.isMarkDirtyByDefault();
        assert test.<JsonMeta>get("test1").isMarkDirtyByDefault();
    }

    @Test
    public void testListeners() {
        JsonMeta test = new JsonMeta(true);

        test.getOrSet("stats.Bedwars", new JsonMeta()).set("favorite_slots", "this is a test");

        JsonMeta reduced1 = test.reduce();
        test.getRecord().clear();

        test.getOrSet("stats.Bedwars", new JsonMeta()).getOrSet("favourites_2", "also a test");

        JsonMeta reduced2 = test.reduce();
        test.getRecord().clear();

        test.getOrSet("stats.Bedwars", new JsonMeta()).set("favourites_2", "also a test");

        JsonMeta reduced3 = test.reduce();
        test.getRecord().clear();

        System.out.println(reduced1.toString());
        System.out.println(reduced2.toString());
        System.out.println(reduced3.toString());
    }
}
