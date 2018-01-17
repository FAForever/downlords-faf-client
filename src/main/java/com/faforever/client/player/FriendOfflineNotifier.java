package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.SocialStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.notification.notificationEvents.ShowTransientNotificationEvent;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Displays a notification whenever a friend goes offline (if enabled in settings).
 */
@Component
public class FriendOfflineNotifier {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final I18n i18n;
  private final EventBus eventBus;
  private final AudioService audioService;
  private final PlayerServiceImpl playerService;

  @Inject
  public FriendOfflineNotifier(ApplicationEventPublisher applicationEventPublisher, I18n i18n, EventBus eventBus,
                               AudioService audioService, PlayerServiceImpl playerService) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.audioService = audioService;
    this.playerService = playerService;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onUserOnline(UserOfflineEvent event) {
    String username = event.getUsername();
    Player player = playerService.getPlayerForUsername(username);
    if (player != null && player.getSocialStatus() == SocialStatus.FRIEND) {
      audioService.playFriendOfflineSound();
      applicationEventPublisher.publishEvent(new ShowTransientNotificationEvent(
          new TransientNotification(
              i18n.get("friend.nowOfflineNotification.title", username), "",
              IdenticonUtil.createIdenticon(player.getId())
          )));
    }
  }
}
