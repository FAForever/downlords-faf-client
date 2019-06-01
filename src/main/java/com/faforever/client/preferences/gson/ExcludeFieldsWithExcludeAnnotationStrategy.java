package com.faforever.client.preferences.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * @see ExcludeFromGson
 */
public class ExcludeFieldsWithExcludeAnnotationStrategy implements ExclusionStrategy {
  @Override
  public boolean shouldSkipClass(Class<?> clazz) {
    return false;
  }

  @Override
  public boolean shouldSkipField(FieldAttributes field) {
    return field.getAnnotation(ExcludeFromGson.class) != null;
  }
}
