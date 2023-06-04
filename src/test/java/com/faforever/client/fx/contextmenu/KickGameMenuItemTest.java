package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.UITest;
import com.faforever.commons.api.dto.GroupPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KickGameMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private ModeratorService moderatorService;

  private KickGameMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new KickGameMenuItem(i18n, moderatorService);
  }

  @Test
  public void testKickGame() {
    PlayerBean player = PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .get();

    instance.setObject(player);
    instance.onClicked();

    verify(moderatorService).closePlayersGame(player);
  }

  @Test
  public void testVisibleItem() {
    when(moderatorService.getPermissions()).thenReturn(Set.of(GroupPermission.ADMIN_KICK_SERVER));
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsSelf() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get());

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPermissions() {
    when(moderatorService.getPermissions()).thenReturn(Set.of(""));
    instance.setObject(PlayerBeanBuilder.create()
        .defaultValues()
        .game(GameBeanBuilder.create().defaultValues().get())
        .get());

    assertFalse(instance.isVisible());
  }
}