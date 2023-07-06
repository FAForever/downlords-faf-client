package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
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
  private PlayerService playerService;

  private RemoveFriendMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new RemoveFriendMenuItem(i18n, playerService);
  }

  @Test
  public void testRemoveFriendIfPlayerIsFriend() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FRIEND).get();

    instance.setObject(player);
    instance.onClicked();

    verify(playerService).removeFriend(player);
  }

  @Test
  public void testVisibleItemIfPlayerIsFriend() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FRIEND).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsOther() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get());
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsFoe() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FOE).get());
    assertFalse(instance.isVisible());
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
}