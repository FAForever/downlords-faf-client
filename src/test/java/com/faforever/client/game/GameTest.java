package com.faforever.client.game;


import com.faforever.client.domain.GameBean;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class GameTest extends ServiceTest {

  private GameBean one;
  private GameBean two;

  @BeforeEach
  public void setUp() throws Exception {
    one = new GameBean();
    two = new GameBean();
  }

  @Test
  public void testEqualsNotEquals() {
    GameBean one = new GameBean();
    GameBean two = new GameBean();

    one.setId(1);
    two.setId(2);

    assertNotEquals(one, two);
  }

  @Test
  public void testEqualsIsEquals() {
    GameBean one = new GameBean();
    GameBean two = new GameBean();

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
