package com.faforever.client.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReflectionUtilTest {

  @Test
  public void testGetDeclaredFieldPublic() throws NoSuchFieldException {
    Class<?> publicString = ReflectionUtil.getDeclaredField("publicString", TestClass.class);
    assertThat((publicString == String.class), is(true));

  }

  @Test
  public void testGetDeclaredFieldPrivate() throws NoSuchFieldException {
    assertThrows(NoSuchFieldException.class, () -> ReflectionUtil.getDeclaredField("private", TestClass.class));
  }

  @Test
  public void testGetDeclaredFieldException() throws NoSuchFieldException {
    assertThrows(NoSuchFieldException.class, () -> ReflectionUtil.getDeclaredField("NoneExisting", TestClass.class));
  }


  public class TestClass {
    private String privateString = "private";
    public String publicString = "public";

  }
}
