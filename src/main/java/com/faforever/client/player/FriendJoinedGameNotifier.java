package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.game.Game;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.events.TransientNotificationEvent;
import com.faforever.client.player.event.FriendJoinedGameEvent;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Displays a notification whenever a friend joins a preferences (if enabled in settings).
 */
@Component
@RequiredArgsConstructor
public class FriendJoinedGameNotifier implements InitializingBean {

  private final I18n i18n;
  private final EventBus eventBus;
  private final JoinGameHelper joinGameHelper;
  private final PreferencesService preferencesService;
  private final AudioService audioService;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onFriendJoinedGame(FriendJoinedGameEvent event) {
    Player player = event.getPlayer();
    Game game = event.getGame();
    if (preferencesService.getPreferences().getNotification().isFriendJoinsGameSoundEnabled()) {
      audioService.playFriendJoinsGameSound();
    }

    if (preferencesService.getPreferences().getNotification().isFriendJoinsGameToastEnabled()) {
      eventBus.post(new TransientNotificationEvent(
          i18n.get("friend.joinedGameNotification.title", player.getUsername(), game.getTitle()),
          i18n.get("friend.joinedGameNotification.action"),
          IdenticonUtil.createIdenticon(player.getId()),
          event1 -> joinGameHelper.join(player.getGame())
      ));
    }
  }
}
