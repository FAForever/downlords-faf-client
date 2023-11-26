package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AddFriendMenuItemTest extends PlatformTest {

  @Mock
  private SocialService socialService;
  @Mock
  private I18n i18n;

  private AddFriendMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new AddFriendMenuItem(socialService, i18n);
  }

  @Test
  public void testAddFriendIfPlayerIsFor() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FOE).get();

    instance.setObject(player);
    instance.onClicked();

    verify(socialService).removeFoe(player);
    verify(socialService).addFriend(player);
  }

  @Test
  public void testAddFriendIfPlayerIsOther() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get();

    instance.setObject(player);
    instance.onClicked();

    verify(socialService, never()).removeFoe(player);
    verify(socialService).addFriend(player);
  }

  @Test
  public void testVisibleItemIfPlayerIsFoe() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FOE).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testVisibleItemIfPlayerIsOther() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsFriend() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FRIEND).get());
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