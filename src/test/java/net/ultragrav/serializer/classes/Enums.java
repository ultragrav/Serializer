package net.ultragrav.serializer.classes;

import net.ultragrav.serializer.annotations.EnumSerialization;

public class Enums {
    public enum NoAnnotation {
        SOME_CONSTANT, OTHER_CONSTANT, YET_ANOTHER_CONSTANT
    }

    @EnumSerialization
    public enum AnnotationOrdinal {
        SOME_CONSTANT, OTHER_CONSTANT, YET_ANOTHER_CONSTANT
    }

    @EnumSerialization(useName = true)
    public enum AnnotationName {
        SOME_CONSTANT, OTHER_CONSTANT, YET_ANOTHER_CONSTANT
    }
}
