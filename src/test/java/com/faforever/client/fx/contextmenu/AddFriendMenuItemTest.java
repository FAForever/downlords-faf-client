package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AddFriendMenuItemTest extends UITest {

  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;

  private AddFriendMenuItem instance;

  @BeforeEach
  public void setUp() {
    instance = new AddFriendMenuItem(playerService, i18n);
  }

  @Test
  public void testAddFriendIfPlayerIsFor() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FOE).get();

    instance.setObject(player);
    instance.onClicked();

    verify(playerService).removeFoe(player);
    verify(playerService).addFriend(player);
  }

  @Test
  public void testAddFriendIfPlayerIsOther() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get();

    instance.setObject(player);
    instance.onClicked();

    verify(playerService, never()).removeFoe(player);
    verify(playerService).addFriend(player);
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
}