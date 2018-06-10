package com.faforever.client.game;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class GameTest {

  private Game one;
  private Game two;

  @Before
  public void setUp() throws Exception {
    one = new Game();
    two = new Game();
  }

  @Test
  public void testEqualsNotEquals() {
    Game one = new Game();
    Game two = new Game();

    one.setId(1);
    two.setId(2);

    assertNotEquals(one, two);
  }

  @Test
  public void testEqualsIsEquals() {
    Game one = new Game();
    Game two = new Game();

    one.setId(1);
    two.setId(1);

    assertEquals(one, two);
  }

  @Test
  public void testHashCodeEquals() {
    one.setId(1);
    two.setId(1);

    assertEquals(one.hashCode(), two.hashCode());
  }

  @Test
  public void testHashCodeNotEquals() {
    one.setId(1);
    two.setId(2);

    assertNotEquals(one.hashCode(), two.hashCode());
  }

}
