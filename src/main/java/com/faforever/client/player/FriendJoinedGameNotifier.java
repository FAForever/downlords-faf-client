package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.game.Game;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.event.FriendJoinedGameEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Displays a notification whenever a friend joins a preferences (if enabled in settings).
 */
@Component
public class FriendJoinedGameNotifier {

  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final JoinGameHelper joinGameHelper;
  private final PreferencesService preferencesService;
  private final AudioService audioService;

  @Inject
  public FriendJoinedGameNotifier(NotificationService notificationService, I18n i18n, EventBus eventBus,
                                  JoinGameHelper joinGameHelper, PreferencesService preferencesService,
                                  AudioService audioService) {
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.joinGameHelper = joinGameHelper;
    this.preferencesService = preferencesService;
    this.audioService = audioService;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onFriendJoinedGame(FriendJoinedGameEvent event) {
    Player player = event.getPlayer();
    Game game = event.getGame();

    audioService.playFriendJoinsGameSound();

    if (preferencesService.getPreferences().getNotification().isFriendJoinsGameToastEnabled()) {
      notificationService.addNotification(new TransientNotification(
          i18n.get("friend.joinedGameNotification.title", player.getUsername(), game.getTitle()),
          i18n.get("friend.joinedGameNotification.action"),
          IdenticonUtil.createIdenticon(player.getId()),
          event1 -> joinGameHelper.join(player.getGame())
      ));
    }
  }
}
