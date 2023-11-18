package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.ClanBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.ChatService;
import com.faforever.client.clan.ClanService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SendPrivateMessageClanLeaderMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private ClanService clanService;
  @Mock
  private ChatService chatService;

  private SendPrivateMessageClanLeaderMenuItem instance;

  @BeforeEach
  public void setup() {
    instance = new SendPrivateMessageClanLeaderMenuItem(i18n, clanService, chatService);
  }

  @Test
  public void testSendMessageClanLeader() {
    when(clanService.getClanByTag(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(ClanBeanBuilder.create()
        .defaultValues()
        .get())));

    instance.setObject(PlayerBeanBuilder.create().get());
    instance.onClicked();
    verify(chatService).onInitiatePrivateChat(any());
  }

  @Test
  public void testVisibleItem() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    instance.setObject(player);
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemWhenNoClan() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testGetItemText() {
    instance.getItemText();
    verify(i18n).get(any());
  }
}