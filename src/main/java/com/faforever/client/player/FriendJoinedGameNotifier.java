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
import javafx.scene.Node;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Displays a notification whenever a friend joins a preferences (if enabled in settings).
 */
@Lazy
@Component
public class FriendJoinedGameNotifier {

  @Inject
  NotificationService notificationService;
  @Inject
  I18n i18n;
  @Inject
  EventBus eventBus;
  @Inject
  JoinGameHelper joinGameHelper;
  @Inject
  PreferencesService preferencesService;
  @Inject
  AudioService audioService;

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onFriendJoinedGame(FriendJoinedGameEvent event) {
    Player player = event.getPlayer();
    Game game = player.getGame();

    audioService.playFriendJoinsGameSound();

    if (preferencesService.getPreferences().getNotification().isFriendJoinsGameToastEnabled()) {
      notificationService.addNotification(new TransientNotification(
          i18n.get("friend.joinedGameNotification.title", player.getUsername(), game.getTitle()),
          i18n.get("friend.joinedGameNotification.action"),
          IdenticonUtil.createIdenticon(player.getId()),
          event1 -> {
            joinGameHelper.setParentNode((Node) event1.getTarget());
            joinGameHelper.join(player.getGame());
          }
      ));
    }
  }
}
