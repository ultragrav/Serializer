package net.ultragrav.serializer;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Test
    public void testConcurrencyBasic() {
        JsonMeta mother = new JsonMeta();
        JsonMeta father = new JsonMeta();
        JsonMeta child = new JsonMeta();

        String key = "key";

        mother.set(key, child);

        AtomicBoolean aliceFinished = new AtomicBoolean();
        AtomicBoolean bobFinished = new AtomicBoolean();

        Thread alice = new Thread(() -> {
            // Alice will be switching the child between the two parents.
            boolean withMother = true;
            for (int i = 0; i < 100000; i++) {
                if (withMother) {
                    // Set to father.
                    father.set(key, child);
                    withMother = false;
                } else {
                    // Set to mother.
                    mother.set(key, child);
                    withMother = true;
                }
            }
            aliceFinished.set(true);
        });

        Thread bob = new Thread(() -> {
            // Bob will continuously be setting a value in the child.
            for (int i = 0; i < 100000; i++) {
                child.set("value", "value");
            }
            bobFinished.set(true);
        });

        alice.start();
        bob.start();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!aliceFinished.get() && bobFinished.get()) {
            throw new RuntimeException("Bob finished, Alice did not!");
        } else if (aliceFinished.get() && !bobFinished.get()) {
            throw new RuntimeException("Alice finished, Bob did not!");
        } else if (!aliceFinished.get() && !bobFinished.get()) {
            throw new RuntimeException("Neither Alice nor Bob finished!");
        }
    }
}
