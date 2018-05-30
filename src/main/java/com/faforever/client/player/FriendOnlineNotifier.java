package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Displays a notification whenever a friend comes online (if enabled in settings).
 */
@Component
public class FriendOnlineNotifier {

  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final AudioService audioService;
  private final PlayerService playerService;
  private final PreferencesService preferencesService;

  @Inject
  public FriendOnlineNotifier(NotificationService notificationService, I18n i18n, EventBus eventBus,
                              AudioService audioService, PlayerService playerService, PreferencesService preferencesService) {
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.audioService = audioService;
    this.playerService = playerService;
    this.preferencesService = preferencesService;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onUserOnline(PlayerOnlineEvent event) {
    NotificationsPrefs notification = preferencesService.getPreferences().getNotification();
    Player player = event.getPlayer();

    if (player.getSocialStatus() != SocialStatus.FRIEND) {
      return;
    }

    if (notification.isFriendOnlineSoundEnabled()) {
      audioService.playFriendOnlineSound();
    }

    if (notification.isFriendOnlineToastEnabled()) {
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
