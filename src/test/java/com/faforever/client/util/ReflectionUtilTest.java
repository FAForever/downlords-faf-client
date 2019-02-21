package com.faforever.client.util;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ReflectionUtilTest {


  @Rule
  public ExpectedException exceptionGrabber = ExpectedException.none();

  @Test
  public void testGetDeclaredFieldPublic() throws NoSuchFieldException {
    Class<?> publicString = ReflectionUtil.getDeclaredField("publicString", TestClass.class);
    assertThat((publicString == String.class), is(true));

  }

  @Test
  @Ignore // this will end in a endless loop.
  public void testGetDeclaredFieldPrivate() throws NoSuchFieldException {
    /*
        The Method will first look if the field is inside the given class.
        If not it will look inside the Superclass.
        Then, if it's found nothing, it will sets its loop variable (currently the Superclass of the given one),
        again to the superclass of the given class. This results into a endless loop because every class has Object as
        superclass (except Object of course) so it will always find Object.class as Superclass and checks like
        while(true) if the superclass has the field (worst case, Object.class)
     */
    exceptionGrabber.expect(NoSuchFieldException.class);
    Class<?> privateString = ReflectionUtil.getDeclaredField("private", TestClass.class);
    assertThat((privateString == String.class), is(true)); // Maybe, if private fields should be looked up with this
  }

  @Test
  @Ignore // this will end in a endless loop.
  public void testGetDeclaredFieldException() throws NoSuchFieldException {

    exceptionGrabber.expect(NoSuchFieldException.class);
    Class<?> privateString = ReflectionUtil.getDeclaredField("NoneExisting", TestClass.class);
  }


  public class TestClass {
    private String privateString = "private";
    public String publicString = "public";

  }
}
