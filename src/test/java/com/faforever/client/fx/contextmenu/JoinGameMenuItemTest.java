package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class JoinGameMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private JoinGameHelper joinGameHelper;

  private JoinGameMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new JoinGameMenuItem(i18n, joinGameHelper);
  }

  @Test
  public void testJoinGame() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username("junit")
        .game(GameBeanBuilder.create().host("junit").status(GameStatus.OPEN).get()).get();

    instance.setObject(player);
    instance.onClicked();

    verify(joinGameHelper).join(player.getGame());
  }

  @Test
  public void testVisibleItemWhenPlayerIsHosting() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().username("junit")
        .game(GameBeanBuilder.create().host("junit").status(GameStatus.OPEN).get()).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testVisibleItemWhenPlayerIsInLobby() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().username("junit")
        .game(GameBeanBuilder.create().host("anotherJunit").status(GameStatus.OPEN).get()).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenPlayerIsIdle() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}