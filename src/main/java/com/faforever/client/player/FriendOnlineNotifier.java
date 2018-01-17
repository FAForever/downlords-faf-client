package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.chat.SocialStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.NavigateEvent;
import com.faforever.client.main.NavigationItem;
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
 * Displays a notification whenever a friend comes online (if enabled in settings).
 */
@Component
public class FriendOnlineNotifier {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final I18n i18n;
  private final EventBus eventBus;
  private final AudioService audioService;
  private final PlayerServiceImpl playerService;

  @Inject
  public FriendOnlineNotifier(ApplicationEventPublisher applicationEventPublisher, I18n i18n, EventBus eventBus,
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
  public void onUserOnline(UserOnlineEvent event) {
    String username = event.getUsername();
    Player player = playerService.getPlayerForUsername(username);
    if (player != null && player.getSocialStatus() == SocialStatus.FRIEND) {
      audioService.playFriendOnlineSound();
      applicationEventPublisher.publishEvent(new ShowTransientNotificationEvent(
          new TransientNotification(
              i18n.get("friend.nowOnlineNotification.title", username),
              i18n.get("friend.nowOnlineNotification.action"),
              IdenticonUtil.createIdenticon(player.getId()),
              actionEvent -> {
                eventBus.post(new NavigateEvent(NavigationItem.CHAT));
                eventBus.post(new InitiatePrivateChatEvent(username));
              }
          )));
    }
  }
}
