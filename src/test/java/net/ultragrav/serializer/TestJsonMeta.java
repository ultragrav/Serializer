package net.ultragrav.serializer;

import net.ultragrav.serializer.classes.JsonMetaSerializables;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

        for (int it = 0; it < 20; it++) {
            JsonMeta mother = new JsonMeta();
            JsonMeta father = new JsonMeta();
            JsonMeta child = new JsonMeta();

            String key = "key";

            mother.set(key, child);

            CompletableFuture<Void> aliceFinished = new CompletableFuture<>();
            CompletableFuture<Void> bobFinished = new CompletableFuture<>();

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
                aliceFinished.complete(null);
            });

            Thread bob = new Thread(() -> {
                // Bob will continuously be setting a value in the child.
                for (int i = 0; i < 100000; i++) {
                    child.set("value", "value");
                }
                bobFinished.complete(null);
            });

            alice.start();
            bob.start();

            try {
                CompletableFuture.allOf(aliceFinished, bobFinished).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }

            if (!aliceFinished.isDone() && bobFinished.isDone()) {
                throw new RuntimeException("Bob finished, Alice did not!");
            } else if (aliceFinished.isDone() && !bobFinished.isDone()) {
                throw new RuntimeException("Alice finished, Bob did not!");
            } else if (!aliceFinished.isDone() && !bobFinished.isDone()) {

                // Print out the stack trace for both threads.
                StackTraceElement[] aliceStack = alice.getStackTrace();
                System.out.println("Alice's stack trace:");
                for (StackTraceElement element : aliceStack) {
                    System.out.println(element);
                }

                StackTraceElement[] bobStack = bob.getStackTrace();
                System.out.println("Bob's stack trace:");
                for (StackTraceElement element : bobStack) {
                    System.out.println(element);
                }

                throw new RuntimeException("Neither Alice nor Bob finished!");

            } else {
//                System.out.println("Both threads finished!");
            }
        }
    }

    @Test
    public void testPutAll() {
        JsonMeta testOne = new JsonMeta(true);
        testOne.getOrSet("stats.Bedwars", new JsonMeta()).set("Experience", 500);

        JsonMeta testTwo = testOne.copy();

        testOne.getRecord().clear();

        testOne.getOrSet("stats.Bedwars", new JsonMeta()).set("Test2", "Test");

        JsonMeta reduced = testOne.reduce();


        testTwo.putAll(reduced);
        System.out.println(testTwo);
    }

    @Test
    public void testFluidTypes() {
        JsonMeta testOne = new JsonMeta(true);
        testOne.getOrSet("stats.Bedwars", new JsonMeta()).set("Experience", 500);

        System.out.println(testOne.getMap("stats"));
    }

    @Test
    public void testMaps() {
        JsonMeta testMeta = new JsonMeta(false);
        Map<ElementType, Integer> testMap = new HashMap<>();

        // Empty map test
        testMeta.set("emptyMap", testMap);
        Map<ElementType, Integer> emptyMapGet = testMeta.getMap("emptyMap");

    }

    @Test
    public void testFailedDeserialization() {
        TestMetaSerializable.TestClassParams object = new TestMetaSerializable.TestClassParams(UUID.randomUUID(), "Test");

        JsonMeta meta = new JsonMeta();
        meta.set("test", object);

        try {
            meta.get("test");
        } catch(ObjectDeserializationException ignored) {}

        JsonMetaSerializables.TestClassParams deserialized = meta.get("test", UUID.randomUUID());

        assert object.getName().equals(deserialized.getName());
    }
}
