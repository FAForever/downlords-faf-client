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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AddFoeMenuItemTest extends UITest {

  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;

  private AddFoeMenuItem instance;

  @BeforeEach
  public void setUp() {
    instance = new AddFoeMenuItem(playerService, i18n);
  }

  @Test
  public void testAddFoeIfPlayerIsFriend() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FRIEND).get();

    instance.onClicked(player);

    verify(playerService).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testAddFoeIfPlayerIsOther() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get();

    instance.onClicked(player);

    verify(playerService, never()).removeFriend(player);
    verify(playerService).addFoe(player);
  }

  @Test
  public void testVisibleItemIfPlayerIsFriend() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.FRIEND).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testVisibleItemIfPlayerIsOther() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get());
    assertTrue(instance.isVisible());
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
}