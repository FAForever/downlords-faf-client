package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Displays a notification whenever a friend comes online (if enabled in settings).
 */
@Component
@RequiredArgsConstructor
public class FriendOnlineNotifier implements InitializingBean {

  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final AudioService audioService;
  private final NotificationPrefs notificationPrefs;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onPlayerOnline(PlayerOnlineEvent event) {
    PlayerBean player = event.getPlayer();

    if (player.getSocialStatus() != SocialStatus.FRIEND) {
      return;
    }

    if (notificationPrefs.isFriendOnlineSoundEnabled()) {
      audioService.playFriendOnlineSound();
    }

    if (notificationPrefs.isFriendOnlineToastEnabled()) {
      notificationService.addNotification(
          new TransientNotification(
              i18n.get("friend.nowOnlineNotification.title", player.getUsername()),
              i18n.get("friend.nowOnlineNotification.action"),
              IdenticonUtil.createIdenticon(player.getId()),
              actionEvent -> {
                eventBus.post(new NavigateEvent(NavigationItem.CHAT));
                eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
              }
          ));
    }
  }
}
