package com.faforever.client.game;


import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class GameTest extends ServiceTest {

  private GameInfo one;
  private GameInfo two;

  @BeforeEach
  public void setUp() throws Exception {
    one = new GameInfo();
    two = new GameInfo();
  }

  @Test
  public void testEqualsNotEquals() {
    GameInfo one = new GameInfo();
    GameInfo two = new GameInfo();

    one.setId(1);
    two.setId(2);

    assertNotEquals(one, two);
  }

  @Test
  public void testEqualsIsEquals() {
    GameInfo one = new GameInfo();
    GameInfo two = new GameInfo();

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
