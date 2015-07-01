package com.faforever.client.test;

import java.math.BigDecimal;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


/**
 * {@link Matcher} that checks a {@link BigDecimal} for its value.
 */
public final class BigDecimalValueMatcher extends TypeSafeMatcher<BigDecimal> {

  private final BigDecimal expectedValue;

  /**
   * Constructor.
   *
   * @param expectedValue the expected {@link BigDecimal} value
   */
  private BigDecimalValueMatcher(BigDecimal expectedValue) {
    this.expectedValue = expectedValue;
  }

  /**
   * Creates a matcher that checks that checks a {@link BigDecimal} for its value.
   *
   * @param expectedValue the expected BigDecimal value
   *
   * @return a matcher that checks that checks a {@link BigDecimal} for its value.
   */
  @Factory
  public static Matcher<BigDecimal> bigDecimalValueEqualTo(BigDecimal expectedValue) {
    return new BigDecimalValueMatcher(expectedValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void describeTo(Description description) {
    description.appendText("has value: " + this.expectedValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean matchesSafely(BigDecimal bigDecimal) {
    // null check not required as it's done by the super class
    return this.expectedValue.compareTo(bigDecimal) == 0;
  }
}
