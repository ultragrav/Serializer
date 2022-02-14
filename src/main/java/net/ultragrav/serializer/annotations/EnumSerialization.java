package net.ultragrav.serializer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate an enum with this class to force serialization by name instead of ordinal
 * (deserializing by ordinal will still work)
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnumSerialization {
    boolean useName() default false;
}
