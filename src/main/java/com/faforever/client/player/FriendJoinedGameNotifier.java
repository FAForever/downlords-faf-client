package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Displays a notification whenever a friend joins a game (if enabled in settings).
 */
public class FriendJoinedGameNotifier {

  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  EventBus eventBus;
  @Resource
  JoinGameHelper joinGameHelper;
  @Resource
  GameService gameService;
  @Resource
  PreferencesService preferencesService;

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onFriendJoinedGame(FriendJoinedGameEvent event) {
    if (!preferencesService.getPreferences().getNotification().getFriendJoinsGameToastEnabled()) {
      return;
    }

    PlayerInfoBean player = event.getPlayerInfoBean();
    // FIXME if this event is triggered before the game has been added to the game service, GameInfoBean is null
    GameInfoBean game = gameService.getByUid(player.getGameUid());

    notificationService.addNotification(new TransientNotification(
        i18n.get("friend.joinedGameNotification.title", player.getUsername(), game.getTitle()),
        i18n.get("friend.joinedGameNotification.action"),
        IdenticonUtil.createIdenticon(player.getId()),
        event1 -> {
          joinGameHelper.setParentNode((Node) event1.getTarget());
          joinGameHelper.join(player.getGameUid());
        }
    ));
  }
}
