package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.SocialStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Displays a notification whenever a friend goes offline (if enabled in settings).
 */
@Component
public class FriendOfflineNotifier {

  @Inject
  NotificationService notificationService;
  @Inject
  I18n i18n;
  @Inject
  EventBus eventBus;
  @Inject
  PreferencesService preferencesService;
  @Inject
  AudioService audioService;
  @Inject
  PlayerServiceImpl playerService;

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onUserOnline(UserOnlineEvent event) {
    String username = event.getUsername();
    Player player = playerService.getPlayerForUsername(username);
    if (player != null && player.getSocialStatus() == SocialStatus.FRIEND) {
      audioService.playFriendOfflineSound();
      notificationService.addNotification(
          new TransientNotification(
              i18n.get("friend.nowOfflineNotification.title", username), "",
              IdenticonUtil.createIdenticon(player.getId())
          ));
    }
  }
}
