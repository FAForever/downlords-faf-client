package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class JoinGameMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private GameRunner gameRunner;

  private JoinGameMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new JoinGameMenuItem(i18n, gameRunner);
  }

  @Test
  public void testJoinGame() {
    PlayerInfo player = PlayerInfoBuilder.create()
                                         .defaultValues()
                                         .username("junit")
                                         .game(GameInfoBuilder.create().host("junit").status(GameStatus.OPEN).get())
                                         .get();

    instance.setObject(player);
    instance.onClicked();

    verify(gameRunner).join(player.getGame());
  }

  @Test
  public void testVisibleItemWhenPlayerIsHosting() {
    instance.setObject(PlayerInfoBuilder.create()
                                        .defaultValues()
                                        .username("junit")
                                        .game(GameInfoBuilder.create().host("junit").status(GameStatus.OPEN).get())
                                        .get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testVisibleItemWhenPlayerIsInLobby() {
    instance.setObject(PlayerInfoBuilder.create()
                                        .defaultValues()
                                        .username("junit")
                                        .game(
                                            GameInfoBuilder.create().host("anotherJunit").status(GameStatus.OPEN).get())
                                        .get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenPlayerIsIdle() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}