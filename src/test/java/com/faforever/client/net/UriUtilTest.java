package com.faforever.client.net;

import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;

import static com.faforever.client.test.IsUtilityClassMatcher.isUtilityClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UriUtilTest extends ServiceTest {

  @Test
  public void testFromStringValid() throws Exception {
    UriUtil.fromString("http://localhost");
  }

  @Test
  public void testFromStringInvalid() throws Exception {
    assertThrows(RuntimeException.class, () -> UriUtil.fromString("^"));
  }

  @Test
  public void testIsUtilityClass() throws Exception {
    assertThat(UriUtil.class, isUtilityClass());
  }
}
