package com.faforever.client.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * {@link Matcher} that checks that a utility class is non-instantiable. Utility classes are supposed to declare a
 * private constructor throwing an {@link AssertionError} in order to prevent instantiation even via reflection.
 */
public class IsUtilityClassMatcher extends TypeSafeMatcher<Class<?>> {

  @Override
  public boolean matchesSafely(Class<?> clazz) {

    if (!Modifier.isFinal(clazz.getModifiers())) {
      return false;
    }

    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    if (constructors.length != 1) {
      return false;
    }

    Constructor<?> constructor = constructors[0];
    try {
      constructor.setAccessible(true);
      constructor.newInstance();
    } catch (ReflectiveOperationException e) {
      return true;
    }
    return false;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("is a utility class");
  }

  /**
   * Matches if the class is not instantiable.
   */
  public static Matcher<Class<?>> isUtilityClass() {
    return new IsUtilityClassMatcher();
  }

}
