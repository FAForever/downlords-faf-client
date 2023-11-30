package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.ChatService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Displays a notification whenever a friend comes online (if enabled in settings).
 */
@Component
@RequiredArgsConstructor
public class FriendOnlineNotifier implements InitializingBean {

  private final PlayerService playerService;
  private final ChatService chatService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final AudioService audioService;
  private final NotificationPrefs notificationPrefs;

  @VisibleForTesting
  void onPlayerOnline(PlayerBean player) {
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
                chatService.onInitiatePrivateChat(player.getUsername());
              }
          ));
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    playerService.addPlayerOnlineListener(this::onPlayerOnline);
  }
}
