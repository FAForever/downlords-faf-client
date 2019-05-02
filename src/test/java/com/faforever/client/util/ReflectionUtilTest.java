package com.faforever.client.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReflectionUtilTest {


  @Rule
  public ExpectedException exceptionGrabber = ExpectedException.none();

  @Test
  public void testGetDeclaredFieldPublic() throws NoSuchFieldException {
    Class<?> publicString = ReflectionUtil.getDeclaredField("publicString", TestClass.class);
    assertThat((publicString == String.class), is(true));

  }

  @Test
  public void testGetDeclaredFieldPrivate() throws NoSuchFieldException {
    exceptionGrabber.expect(NoSuchFieldException.class);
    ReflectionUtil.getDeclaredField("private", TestClass.class);
  }

  @Test
  public void testGetDeclaredFieldException() throws NoSuchFieldException {

    exceptionGrabber.expect(NoSuchFieldException.class);
    ReflectionUtil.getDeclaredField("NoneExisting", TestClass.class);
  }


  public class TestClass {
    private String privateString = "private";
    public String publicString = "public";

  }
}
