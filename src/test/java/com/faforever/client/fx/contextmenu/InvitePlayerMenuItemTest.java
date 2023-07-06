package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InvitePlayerMenuItemTest extends PlatformTest {

  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private I18n i18n;

  private InvitePlayerMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new InvitePlayerMenuItem(i18n, teamMatchmakingService);
  }

  @Test
  public void testInvitePlayer() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();

    instance.setObject(player);
    instance.onClicked();

    Mockito.verify(teamMatchmakingService).invitePlayer(player.getUsername());
  }

  @Test
  public void testInvisibleItemWhenPlayerIsIdle() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues()
        .game(GameBeanBuilder.create().status(GameStatus.PLAYING).get()).get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testVisibleItemWhenPlayerIsIdle() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}