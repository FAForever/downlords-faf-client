package com.faforever.client.fx.contextmenu;

import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractCustomMenuItemControllerTest extends UITest {

  private AbstractCustomMenuItemController<Object> instance;

  @BeforeEach
  public void setUp() {
    instance = new AbstractCustomMenuItemController<>() {
      @Override
      public void afterSetObject(Object object) {

      }
    };
  }

  @Test
  public void testSetObject() {
    Object object = new Object();
    instance.setObject(object);
    assertEquals(object, instance.getObject());
  }
}