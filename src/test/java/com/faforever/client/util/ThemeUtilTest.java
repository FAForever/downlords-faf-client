package com.faforever.client.util;

import org.junit.Test;

import static com.faforever.client.test.IsUtilityClassMatcher.isUtilityClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class ThemeUtilTest {

  @Test
  public void testUtilityClass() {
    assertThat(SocketAddressUtil.class, isUtilityClass());
  }

  @Test
  public void testThemeFile() throws Exception {
    String themeFile = ThemeUtil.themeFile("junit", "relativeFile.xml");

    assertEquals("/themes/junit/relativeFile.xml", themeFile);
  }
}
