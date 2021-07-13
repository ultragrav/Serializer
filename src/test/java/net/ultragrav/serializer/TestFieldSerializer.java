package net.ultragrav.serializer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TestFieldSerializer {
    public static class TestClassOne {
        private String strField = "def";
        private int iField = 1;
        private Map<String, Object> oField;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClassOne that = (TestClassOne) o;
            return iField == that.iField &&
                    Objects.equals(strField, that.strField) &&
                    Objects.equals(oField, that.oField);
        }

        @Override
        public int hashCode() {
            return Objects.hash(strField, iField, oField);
        }
    }

    public static class TestClassTwoA {
        private String name;
        private TestClassTwoB other;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClassTwoA that = (TestClassTwoA) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(other, that.other);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, other);
        }
    }

    public static class TestClassTwoB {
        private String name;
        private UUID id;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClassTwoB that = (TestClassTwoB) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, id);
        }
    }

    @Test
    public void testOne() {
        TestClassOne test = new TestClassOne();
        test.strField = "notDef";
        test.iField = 3;
        test.oField = new HashMap<>();
        test.oField.put("Oof", 19L);

        Meta meta = FieldSerializer.serializeFields(test, false);

        TestClassOne other = new TestClassOne();
        FieldSerializer.deserializeFields(other, meta);
        assert Objects.equals(test, other);
    }

    @Test
    public void testTwoNormal() {
        TestClassTwoA a1 = new TestClassTwoA();
        a1.name = "firstA";
        TestClassTwoB b1 = new TestClassTwoB();
        b1.name = "firstB";
        b1.id = UUID.randomUUID();
        a1.other = b1;

        // Ensure that non-recursive serialization allows deserialization
        Meta meta = FieldSerializer.serializeFields(a1, false);

        TestClassTwoA a2 = new TestClassTwoA();
        FieldSerializer.deserializeFields(a2, meta);

        assert Objects.equals(a1, a2);
    }

    @Test
    public void testTwoRecursive() {
        TestClassTwoA a1 = new TestClassTwoA();
        a1.name = "firstA";
        TestClassTwoB b1 = new TestClassTwoB();
        b1.name = "firstB";
        b1.id = UUID.randomUUID();
        a1.other = b1;

        // Test if recursively serialized object serializes to bytes correctly
        Meta meta = FieldSerializer.serializeFields(a1, true);

        GravSerializer ser = new GravSerializer();
        meta.serialize(ser);

        meta = new Meta(ser);

        TestClassTwoA a3 = new TestClassTwoA();
        FieldSerializer.deserializeFields(a3, meta);

        assert Objects.equals(a1, a3);
    }
}
