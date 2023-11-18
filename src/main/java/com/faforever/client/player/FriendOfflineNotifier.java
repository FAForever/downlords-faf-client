package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.util.IdenticonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Displays a notification whenever a friend goes offline (if enabled in settings).
 */
@Component
@RequiredArgsConstructor
public class FriendOfflineNotifier implements InitializingBean {

  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final I18n i18n;
  private final AudioService audioService;
  private final NotificationPrefs notificationPrefs;

  private void onPlayerOffline(PlayerBean player) {
    if (player.getSocialStatus() != SocialStatus.FRIEND) {
      return;
    }

    if (notificationPrefs.isFriendOfflineSoundEnabled()) {
      audioService.playFriendOfflineSound();
    }

    if (notificationPrefs.isFriendOfflineToastEnabled()) {
      notificationService.addNotification(
          new TransientNotification(
              i18n.get("friend.nowOfflineNotification.title", player.getUsername()), "",
              IdenticonUtil.createIdenticon(player.getId())
          ));
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    playerService.addPlayerOfflineListener(this::onPlayerOffline);
  }
}
