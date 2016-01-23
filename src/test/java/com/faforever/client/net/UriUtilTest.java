package com.faforever.client.net;

import org.junit.Test;

import static com.faforever.client.test.IsUtilityClassMatcher.isUtilityClass;
import static org.hamcrest.MatcherAssert.assertThat;

public class UriUtilTest {

  @Test
  public void testFromStringValid() throws Exception {
    UriUtil.fromString("http://localhost");
  }

  @Test(expected = RuntimeException.class)
  public void testFromStringInvalid() throws Exception {
    UriUtil.fromString("^");
  }

  @Test
  public void testIsUtilityClass() throws Exception {
    assertThat(UriUtil.class, isUtilityClass());
  }
}
