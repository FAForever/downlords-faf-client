package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendPrivateMessageClanLeaderMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private PlayerService playerService;
  @Mock
  private EventBus eventBus;

  @InjectMocks
  private SendPrivateMessageClanLeaderMenuItem instance;

  @Test
  public void testSendMessageClanLeader() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().id(100).get();
    when(playerService.getCurrentPlayer()).thenReturn(player);

    instance.setObject(ClanBeanBuilder.create().defaultValues().get());
    instance.onClicked();
    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testVisibleItem() {
    ClanBean clan = ClanBeanBuilder.create().defaultValues().get();
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(playerService.isOnline(clan.getLeader().getId())).thenReturn(true);
    instance.setObject(clan);
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoClan() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenOwnPlayerIsLeader() {
    ClanBean clan = ClanBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(clan.getLeader());
    instance.setObject(clan);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenLeaderIsOffline() {
    ClanBean clan = ClanBeanBuilder.create().defaultValues().get();
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(playerService.isOnline(clan.getLeader().getId())).thenReturn(false);
    instance.setObject(clan);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testGetItemText() {
    instance.getItemText();
    verify(i18n).get(any());
  }
}