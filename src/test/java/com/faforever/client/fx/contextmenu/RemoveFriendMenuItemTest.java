package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class RemoveFriendMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private SocialService socialService;

  private RemoveFriendMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new RemoveFriendMenuItem(i18n, socialService);
  }

  @Test
  public void testRemoveFriendIfPlayerIsFriend() {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().socialStatus(SocialStatus.FRIEND).get();

    instance.setObject(player);
    instance.onClicked();

    verify(socialService).removeFriend(player);
  }

  @Test
  public void testVisibleItemIfPlayerIsFriend() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().socialStatus(SocialStatus.FRIEND).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsOther() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsFoe() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().socialStatus(SocialStatus.FOE).get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsSelf() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().socialStatus(SocialStatus.SELF).get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}