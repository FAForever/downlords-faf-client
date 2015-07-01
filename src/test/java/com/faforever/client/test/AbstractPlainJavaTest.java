package com.faforever.client.test;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Before;
import org.junit.internal.ArrayComparisonFailure;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import java.math.BigDecimal;

/**
 * Base class for JUnit 4 tests that provides various assertion methods to {@link AbstractTransactionalJUnit4SpringContextTests}
 * and initialize the logger.
 */
public abstract class AbstractPlainJavaTest {

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  /*
   * Methods wrapped from Hamcrest Matchers -> as these are delegates, the JavaDoc is missing where it is missing in
   * the original Matchers class.
   */

  /**
   * Evaluates to true only if ALL of the passed in matchers evaluate to true.
   */
  public static <T> org.hamcrest.Matcher<T> allOf(java.lang.Iterable<org.hamcrest.Matcher<? super T>> matchers) {
    return Matchers.allOf(matchers);
  }

  /**
   * Evaluates to true only if ALL of the passed in matchers evaluate to true.
   */
  @SafeVarargs
  public static <T> org.hamcrest.Matcher<T> allOf(org.hamcrest.Matcher<? super T>... matchers) {
    return Matchers.allOf(matchers);
  }

  /**
   * Evaluates to true if ANY of the passed in matchers evaluate to true.
   */
  @SafeVarargs
  public static <T> org.hamcrest.core.AnyOf<T> anyOf(org.hamcrest.Matcher<? super T>... matchers) {
    return Matchers.anyOf(matchers);
  }

  /**
   * This is useful for fluently combining matchers that must both pass. For example:
   * <p>
   * <pre>
   * assertThat(string, both(containsString(&quot;a&quot;)).and(containsString(&quot;b&quot;)));
   * </pre>
   */
  public static <T> org.hamcrest.core.CombinableMatcher.CombinableBothMatcher<T> both(org.hamcrest.Matcher<? super T> matcher) {
    return Matchers.both(matcher);
  }

  /**
   * This is useful for fluently combining matchers where either may pass. For example:
   * <p>
   * <pre>
   * assertThat(string, both(containsString(&quot;a&quot;)).and(containsString(&quot;b&quot;)));
   * </pre>
   */
  public static <T> org.hamcrest.core.CombinableMatcher.CombinableEitherMatcher<T> either(org.hamcrest.Matcher<? super T> matcher) {
    return Matchers.either(matcher);
  }

  /**
   * Wraps an existing matcher and overrides the description when it fails.
   */
  public static <T> org.hamcrest.Matcher<T> describedAs(java.lang.String description, org.hamcrest.Matcher<T> matcher, java.lang.Object... values) {
    return Matchers.describedAs(description, matcher, values);
  }

  /**
   * @param itemMatcher A matcher to apply to every element in a collection.
   *
   * @return Evaluates to TRUE for a collection in which every item matches itemMatcher
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <U> org.hamcrest.Matcher<java.lang.Iterable<? extends U>> everyItem(org.hamcrest.Matcher<? super U> itemMatcher) {
    return (Matcher) Matchers.everyItem(itemMatcher);
  }

  /**
   * Decorates another Matcher, retaining the behavior but allowing tests to be slightly more expressive. For example:
   * assertThat(cheese, equalTo(smelly)) vs. assertThat(cheese, is(equalTo(smelly)))
   */
  public static <T> org.hamcrest.Matcher<T> is(org.hamcrest.Matcher<T> matcher) {
    return Matchers.is(matcher);
  }

  /**
   * This is a shortcut to the frequently used is(equalTo(x)). For example: assertThat(cheese, is(equalTo(smelly))) vs.
   * assertThat(cheese, is(smelly))
   */
  public static <T> org.hamcrest.Matcher<? super T> is(T value) {
    return Matchers.is(value);
  }

  /**
   * This is a shortcut to the frequently used is(instanceOf(SomeClass.class)). For example: assertThat(cheese,
   * is(instanceOf(Cheddar.class))) vs. assertThat(cheese, is(Cheddar.class))
   */
  public static <T> org.hamcrest.Matcher<? super T> isA(java.lang.Class<T> type) {
    return Matchers.isA(type);
  }

  /**
   * This matcher always evaluates to true.
   */
  public static <T> org.hamcrest.Matcher<T> anything() {
    return (Matcher<T>) Matchers.anything();
  }

  /**
   * This matcher always evaluates to true.
   *
   * @param description A meaningful string used when describing itself.
   */
  public static <T> org.hamcrest.Matcher<T> anything(java.lang.String description) {
    return (Matcher<T>) Matchers.anything(description);
  }

  /**
   * This method was not wrapped due to Hamcrest <a href="http://code.google.com/p/hamcrest/issues/detail?id=100">issue
   * 100</a>.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> hasItem(T element) {
    return new IsCollectionContaining(Matchers.equalTo(element));
  }

  /**
   * This method was not wrapped due to Hamcrest <a href="http://code.google.com/p/hamcrest/issues/detail?id=100">issue
   * 100</a>.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> hasItem(
      org.hamcrest.Matcher<? super T> elementMatcher) {
    return new IsCollectionContaining(elementMatcher);
  }

  @SafeVarargs
  public static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItems(org.hamcrest.Matcher<? super T>... elementMatchers) {
    return Matchers.hasItems(elementMatchers);
  }

  @SafeVarargs
  public static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItems(T... elements) {
    return Matchers.hasItems(elements);
  }

  /**
   * Is the value equal to another value, as tested by the {@link java.lang.Object#equals} invokedMethod?
   */
  public static <T> org.hamcrest.Matcher<? super T> equalTo(T operand) {
    return Matchers.equalTo(operand);
  }

  /**
   * Is the {@link BigDecimal} value equal to another {@link BigDecimal} value?
   */
  public static org.hamcrest.Matcher<BigDecimal> bigDecimalValueEqualTo(BigDecimal operand) {
    return BigDecimalValueMatcher.bigDecimalValueEqualTo(operand);
  }

  /**
   * Is the value an instance of a particular type? This version assumes no relationship between the required type and
   * the signature of the method that sets it up, for example in <code>assertThat(anObject,
   * instanceOf(Thing.class));</code>
   */
  public static <T> org.hamcrest.Matcher<T> instanceOf(java.lang.Class<?> type) {
    return Matchers.instanceOf(type);
  }

  /**
   * Is the value an instance of a particular type? Use this version to make generics conform, for example in the JMock
   * clause <code>with(any(Thing.class))</code>
   */
  public static <T> org.hamcrest.Matcher<T> any(java.lang.Class<T> type) {
    return Matchers.any(type);
  }

  /**
   * Inverts the rule.
   */
  public static <T> org.hamcrest.Matcher<T> not(org.hamcrest.Matcher<T> matcher) {
    return Matchers.not(matcher);
  }

  /**
   * This is a shortcut to the frequently used not(equalTo(x)). For example: assertThat(cheese,
   * is(not(equalTo(smelly)))) vs. assertThat(cheese, is(not(smelly)))
   */
  public static <T> org.hamcrest.Matcher<? super T> not(T value) {
    return Matchers.not(value);
  }

  /**
   * Matches if value is null.
   */
  public static <T> org.hamcrest.Matcher<T> nullValue() {
    return (Matcher<T>) Matchers.nullValue();
  }

  /**
   * Matches if value is null. With type inference.
   */
  public static <T> org.hamcrest.Matcher<T> nullValue(java.lang.Class<T> type) {
    return Matchers.nullValue(type);
  }

  /**
   * Matches if value is not null.
   */
  public static <T> org.hamcrest.Matcher<T> notNullValue() {
    return (Matcher<T>) Matchers.notNullValue();
  }

  /**
   * Matches if value is not null. With type inference.
   */
  public static <T> org.hamcrest.Matcher<T> notNullValue(java.lang.Class<T> type) {
    return Matchers.notNullValue(type);
  }

  /**
   * Creates a new instance of IsSame.
   *
   * @param object The predicate evaluates to true only when the argument is this object.
   */
  public static <T> org.hamcrest.Matcher<T> sameInstance(T object) {
    return Matchers.sameInstance(object);
  }

  public static org.hamcrest.Matcher<java.lang.String> containsString(java.lang.String substring) {
    return org.hamcrest.core.StringContains.containsString(substring);
  }

  public static org.hamcrest.Matcher<java.lang.String> startsWith(java.lang.String substring) {
    return org.hamcrest.core.StringStartsWith.startsWith(substring);
  }

  public static org.hamcrest.Matcher<java.lang.String> endsWith(java.lang.String substring) {
    return org.hamcrest.core.StringEndsWith.endsWith(substring);
  }

  /**
   * Evaluates to true if any item in an array satisfies the given matcher.
   */
  public static <T> org.hamcrest.Matcher<T[]> hasItemInArray(org.hamcrest.Matcher<? super T> elementMatcher) {
    return Matchers.hasItemInArray(elementMatcher);
  }

  /**
   * This is a shortcut to the frequently used hasItemInArray(equalTo(x)). For example,
   * assertThat(hasItemInArray(equal_to(x))) vs. assertThat(hasItemInArray(x))
   */
  public static <T> org.hamcrest.Matcher<T[]> hasItemInArray(T element) {
    return Matchers.hasItemInArray(element);
  }

  @SafeVarargs
  public static <E> org.hamcrest.Matcher<E[]> arrayContaining(E... items) {
    return Matchers.arrayContaining(items);
  }

  @SafeVarargs
  public static <E> org.hamcrest.Matcher<E[]> arrayContaining(org.hamcrest.Matcher<? super E>... matchers) {
    return Matchers.arrayContaining(matchers);
  }

  public static <E> org.hamcrest.Matcher<E[]> arrayContaining(java.util.List<org.hamcrest.Matcher<? super E>> matchers) {
    return Matchers.arrayContaining(matchers);
  }

  @SafeVarargs
  public static <E> org.hamcrest.Matcher<E[]> arrayContainingInAnyOrder(org.hamcrest.Matcher<? super E>... matchers) {
    return Matchers.arrayContainingInAnyOrder(matchers);
  }

  public static <E> org.hamcrest.Matcher<E[]> arrayContainingInAnyOrder(java.util.Collection<org.hamcrest.Matcher<? super E>> matchers) {
    return Matchers.arrayContainingInAnyOrder(matchers);
  }

  @SafeVarargs
  public static <E> org.hamcrest.Matcher<E[]> arrayContainingInAnyOrder(E... items) {
    return Matchers.arrayContainingInAnyOrder(items);
  }

  /**
   * Does array size satisfy a given matcher?
   */
  public static <E> org.hamcrest.Matcher<E[]> arrayWithSize(org.hamcrest.Matcher<? super java.lang.Integer> sizeMatcher) {
    return Matchers.arrayWithSize(sizeMatcher);
  }

  /**
   * This is a shortcut to the frequently used arrayWithSize(equalTo(x)). For example,
   * assertThat(arrayWithSize(equal_to(x))) vs. assertThat(arrayWithSize(x))
   */
  public static <E> org.hamcrest.Matcher<E[]> arrayWithSize(int size) {
    return Matchers.arrayWithSize(size);
  }

  /**
   * Matches an empty array.
   */
  public static <E> org.hamcrest.Matcher<E[]> emptyArray() {
    return Matchers.emptyArray();
  }

  /**
   * Does collection size satisfy a given matcher?
   */
  public static <E> org.hamcrest.Matcher<? super java.util.Collection<? extends E>> hasSize(org.hamcrest.Matcher<? super java.lang.Integer> size) {
    return Matchers.hasSize(size);
  }

  /**
   * This is a shortcut to the frequently used hasSize(equalTo(x)). For example, assertThat(hasSize(equal_to(x))) vs.
   * assertThat(hasSize(x))
   */
  public static <E> org.hamcrest.Matcher<? super java.util.Collection<? extends E>> hasSize(int size) {
    return Matchers.hasSize(size);
  }

  /**
   * Matches an empty iterable.
   */
  public static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> emptyIterable() {
    return Matchers.emptyIterable();
  }

  @SafeVarargs
  public static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> contains(org.hamcrest.Matcher<? super E>... item) {
    return Matchers.contains(item);
  }

  public static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> contains(java.util.List<org.hamcrest.Matcher<? super E>> contents) {
    return Matchers.contains(contents);
  }

  @SafeVarargs
  public static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> contains(E... items) {
    return Matchers.contains(items);
  }

  public static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> containsInAnyOrder(java.util.Collection<org.hamcrest.Matcher<? super T>> matchers) {
    return Matchers.containsInAnyOrder(matchers);
  }

  @SafeVarargs
  public static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> containsInAnyOrder(org.hamcrest.Matcher<? super T>... item) {
    return Matchers.containsInAnyOrder(item);
  }

  @SafeVarargs
  public static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> containsInAnyOrder(T... items) {
    return Matchers.containsInAnyOrder(items);
  }

  public static <E> org.hamcrest.Matcher<java.lang.Iterable<E>> iterableWithSize(org.hamcrest.Matcher<? super java.lang.Integer> sizeMatcher) {
    return Matchers.iterableWithSize(sizeMatcher);
  }

  public static <E> org.hamcrest.Matcher<java.lang.Iterable<E>> iterableWithSize(int size) {
    return Matchers.iterableWithSize(size);
  }

  public static <K, V> org.hamcrest.Matcher<java.util.Map<? extends K, ? extends V>> hasEntry(
      org.hamcrest.Matcher<? super K> keyMatcher, org.hamcrest.Matcher<? super V> valueMatcher) {
    return Matchers.<K, V>hasEntry(keyMatcher, valueMatcher);
  }

  public static <K, V> org.hamcrest.Matcher<java.util.Map<? extends K, ? extends V>> hasEntry(K key, V value) {
    return Matchers.hasEntry(key, value);
  }

  public static <K> org.hamcrest.Matcher<java.util.Map<? extends K, ?>> hasKey(K key) {
    return Matchers.hasKey(key);
  }

  public static <K> org.hamcrest.Matcher<java.util.Map<? extends K, ?>> hasKey(org.hamcrest.Matcher<? super K> keyMatcher) {
    return Matchers.<K>hasKey(keyMatcher);
  }

  public static <V> org.hamcrest.Matcher<? super java.util.Map<?, V>> hasValue(V value) {
    return Matchers.hasValue(value);
  }

  public static <V> org.hamcrest.Matcher<? super java.util.Map<?, V>> hasValue(org.hamcrest.Matcher<? super V> valueMatcher) {
    return Matchers.hasValue(valueMatcher);
  }

  public static <T> org.hamcrest.Matcher<T> isIn(java.util.Collection<T> collection) {
    return Matchers.isIn(collection);
  }

  public static <T> org.hamcrest.Matcher<T> isIn(T[] param1) {
    return Matchers.isIn(param1);
  }

  @SafeVarargs
  public static <T> org.hamcrest.Matcher<T> isOneOf(T... elements) {
    return Matchers.isOneOf(elements);
  }

  public static org.hamcrest.Matcher<java.lang.Double> closeTo(double operand, double error) {
    return org.hamcrest.number.IsCloseTo.closeTo(operand, error);
  }

  /**
   * Is value = expected?
   */
  public static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<? super T> comparesEqualTo(T value) {
    return Matchers.comparesEqualTo(value);
  }

  /**
   * Is value > expected?
   */
  public static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<? super T> greaterThan(T value) {
    return Matchers.greaterThan(value);
  }

  /**
   * Is value >= expected?
   */
  public static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<? super T> greaterThanOrEqualTo(T value) {
    return Matchers.greaterThanOrEqualTo(value);
  }

  /**
   * Is value < expected?
   */
  public static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<? super T> lessThan(T value) {
    return Matchers.lessThan(value);
  }

  /**
   * Is value <= expected?
   */
  public static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<? super T> lessThanOrEqualTo(T value) {
    return Matchers.lessThanOrEqualTo(value);
  }

  public static org.hamcrest.Matcher<java.lang.String> equalToIgnoringCase(java.lang.String string) {
    return Matchers.equalToIgnoringCase(string);
  }

  public static org.hamcrest.Matcher<java.lang.String> equalToIgnoringWhiteSpace(java.lang.String string) {
    return Matchers.equalToIgnoringWhiteSpace(string);
  }

  /**
   * Evaluates whether item.toString() satisfies a given matcher.
   */
  public static <T> org.hamcrest.Matcher<T> hasToString(org.hamcrest.Matcher<? super java.lang.String> toStringMatcher) {
    return Matchers.hasToString(toStringMatcher);
  }

  /**
   * This is a shortcut to the frequently used has_string(equalTo(x)). For example, assertThat(hasToString(equal_to(x)))
   * vs. assertThat(hasToString(x))
   */
  public static <T> org.hamcrest.Matcher<T> hasToString(java.lang.String expectedToString) {
    return Matchers.hasToString(expectedToString);
  }

  public static <T> org.hamcrest.Matcher<java.lang.Class<?>> typeCompatibleWith(java.lang.Class<T> baseType) {
    return Matchers.typeCompatibleWith(baseType);
  }

  public static <T> org.hamcrest.Matcher<T> hasProperty(java.lang.String propertyName) {
    return Matchers.hasProperty(propertyName);
  }

  public static <T> org.hamcrest.Matcher<T> hasProperty(java.lang.String propertyName, org.hamcrest.Matcher<?> value) {
    return Matchers.hasProperty(propertyName, value);
  }

  public static Matcher<Class<?>> isUtilityClass() {
    return IsUtilityClassMatcher.isUtilityClass();
  }

  /**
   * Methods wrapped from Assert.
   */

  /**
   * @see Assert#assertTrue(String, boolean)
   */
  public static void assertTrue(String message, boolean condition) {
    Assert.assertTrue(message, condition);
  }

  /**
   * @see Assert#assertTrue(boolean)
   */
  public static void assertTrue(boolean condition) {
    Assert.assertTrue(condition);
  }

  /**
   * @see Assert#assertFalse(String, boolean)
   */
  public static void assertFalse(String message, boolean condition) {
    Assert.assertFalse(message, condition);
  }

  /**
   * @see Assert#assertFalse(boolean)
   */
  public static void assertFalse(boolean condition) {
    Assert.assertFalse(condition);
  }

  /**
   * @see Assert#fail(String)
   */
  public static void fail(String message) {
    Assert.fail(message);
  }

  /**
   * @see Assert#fail()
   */
  public static void fail() {
    Assert.fail();
  }

  /**
   * @see Assert#assertEquals(String, Object, Object)
   */
  public static void assertEquals(String message, Object expected, Object actual) {
    Assert.assertEquals(message, expected, actual);
  }

  /**
   * @see Assert#assertEquals(Object, Object)
   */
  public static void assertEquals(Object expected, Object actual) {
    Assert.assertEquals(expected, actual);
  }

  /**
   * @see Assert#assertArrayEquals(String, Object[], Object[])
   */
  public static void assertArrayEquals(String message, Object[] expecteds, Object[] actuals) throws ArrayComparisonFailure {
    Assert.assertArrayEquals(message, expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(Object[], Object[])
   */
  public static void assertArrayEquals(Object[] expecteds, Object[] actuals) {
    Assert.assertArrayEquals(expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(String, byte[], byte[])
   */
  public static void assertArrayEquals(String message, byte[] expecteds, byte[] actuals) throws ArrayComparisonFailure {
    Assert.assertArrayEquals(message, expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(byte[], byte[])
   */
  public static void assertArrayEquals(byte[] expecteds, byte[] actuals) {
    Assert.assertArrayEquals(expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(String, char[], char[])
   */
  public static void assertArrayEquals(String message, char[] expecteds, char[] actuals) throws ArrayComparisonFailure {
    Assert.assertArrayEquals(message, expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(char[], char[])
   */
  public static void assertArrayEquals(char[] expecteds, char[] actuals) {
    Assert.assertArrayEquals(expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(String, short[], short[])
   */
  public static void assertArrayEquals(String message, short[] expecteds, short[] actuals) throws ArrayComparisonFailure {
    Assert.assertArrayEquals(message, expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(short[], short[])
   */
  public static void assertArrayEquals(short[] expecteds, short[] actuals) {
    Assert.assertArrayEquals(expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(String, int[], int[])
   */
  public static void assertArrayEquals(String message, int[] expecteds, int[] actuals) throws ArrayComparisonFailure {
    Assert.assertArrayEquals(message, expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(int[], int[])
   */
  public static void assertArrayEquals(int[] expecteds, int[] actuals) {
    Assert.assertArrayEquals(expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(String, long[], long[])
   */
  public static void assertArrayEquals(String message, long[] expecteds, long[] actuals) throws ArrayComparisonFailure {
    Assert.assertArrayEquals(message, expecteds, actuals);
  }

  /**
   * @see Assert#assertArrayEquals(long[], long[])
   */
  public static void assertArrayEquals(long[] expecteds, long[] actuals) {
    Assert.assertArrayEquals(expecteds, actuals);
  }

  /**
   * @see Assert#assertEquals(String, double, double, double)
   */
  public static void assertEquals(String message, double expected, double actual, double delta) {
    Assert.assertEquals(message, expected, actual, delta);
  }

  /**
   * @see Assert#assertEquals(long, long)
   */
  public static void assertEquals(long expected, long actual) {
    Assert.assertEquals(expected, actual);
  }

  /**
   * @see Assert#assertEquals(String, long, long)
   */
  public static void assertEquals(String message, long expected, long actual) {
    Assert.assertEquals(message, expected, actual);
  }

  /**
   * @see Assert#assertEquals(double, double, double)
   */
  public static void assertEquals(double expected, double actual, double delta) {
    Assert.assertEquals(expected, actual, delta);
  }

  /**
   * @see Assert#assertNotNull(String, Object)
   */
  public static void assertNotNull(String message, Object object) {
    Assert.assertNotNull(message, object);
  }

  /**
   * @see Assert#assertNotNull(Object)
   */
  public static void assertNotNull(Object object) {
    Assert.assertNotNull(object);
  }

  /**
   * @see Assert#assertNull(String, Object)
   */
  public static void assertNull(String message, Object object) {
    Assert.assertNull(message, object);
  }

  /**
   * @see Assert#assertNull(Object)
   */
  public static void assertNull(Object object) {
    Assert.assertNull(object);
  }

  /**
   * @see Assert#assertSame(String, Object, Object)
   */
  public static void assertSame(String message, Object expected, Object actual) {
    Assert.assertSame(message, expected, actual);
  }

  /**
   * @see Assert#assertSame(Object, Object)
   */
  public static void assertSame(Object expected, Object actual) {
    Assert.assertSame(expected, actual);
  }

  /**
   * @see Assert#assertNotSame(String, Object, Object)
   */
  public static void assertNotSame(String message, Object unexpected, Object actual) {
    Assert.assertNotSame(message, unexpected, actual);
  }

  /**
   * @see Assert#assertNotSame(Object, Object)
   */
  public static void assertNotSame(Object unexpected, Object actual) {
    Assert.assertNotSame(unexpected, actual);
  }

  /**
   * @see Assert#assertThat(Object, Matcher)
   */
  public static <T> void assertThat(T actual, Matcher<T> matcher) {
    Assert.assertThat(actual, matcher);
  }

  /**
   * @see Assert#assertThat(String, Object, Matcher)
   */
  public static <T> void assertThat(String reason, T actual, Matcher<T> matcher) {
    Assert.assertThat(reason, actual, matcher);
  }

}
