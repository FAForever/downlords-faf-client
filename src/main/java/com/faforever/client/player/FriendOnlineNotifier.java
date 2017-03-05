package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.chat.SocialStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
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
  private final PlayerServiceImpl playerService;

  @Inject
  public FriendOnlineNotifier(NotificationService notificationService, I18n i18n, EventBus eventBus, AudioService audioService, PlayerServiceImpl playerService) {
    this.notificationService = notificationService;
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
      notificationService.addNotification(
          new TransientNotification(
              i18n.get("friend.nowOnlineNotification.title", username),
              i18n.get("friend.nowOnlineNotification.action"),
              IdenticonUtil.createIdenticon(player.getId()),
              actionEvent -> eventBus.post(new InitiatePrivateChatEvent(username))
          ));
    }
  }
}
