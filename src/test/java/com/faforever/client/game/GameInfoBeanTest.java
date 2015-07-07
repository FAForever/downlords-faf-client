package com.faforever.client.game;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameInfoBeanTest {

  private GameInfoBean one;
  private GameInfoBean two;

  @Before
  public void setUp() throws Exception {
     one = new GameInfoBean();
     two = new GameInfoBean();
  }

  @Test
  public void testEqualsNotEquals() {
    GameInfoBean one = new GameInfoBean();
    GameInfoBean two = new GameInfoBean();

    one.setUid(1);
    two.setUid(2);

    assertNotEquals(one,two);
  }

  @Test
  public void testEqualsIsEquals() {
    GameInfoBean one = new GameInfoBean();
    GameInfoBean two = new GameInfoBean();

    one.setUid(1);
    two.setUid(1);

    assertEquals(one,two);
  }

  @Test
  public void testHashCode() {
    one.setUid(1);
    two.setUid(2);

    assertEquals(one.hashCode(),two.hashCode());
  }

}