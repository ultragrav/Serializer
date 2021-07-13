package net.ultragrav.serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field with @NoSerialize to prevent it from being
 * serialized by {@link FieldSerializer#serializeFields(Object, boolean)}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSerialize {
}
