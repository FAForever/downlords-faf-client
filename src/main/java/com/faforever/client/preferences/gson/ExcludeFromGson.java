package com.faforever.client.preferences.gson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes fields with this annotation from Gson Serialization and Deserialization if configured accordingly.
 *
 * @see ExcludeFieldsWithExcludeAnnotationStrategy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcludeFromGson {
}