package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.util.IdenticonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Displays a notification whenever a friend joins a preferences (if enabled in settings).
 */
@Component
@RequiredArgsConstructor
public class FriendJoinedGameNotifier {

  private final NotificationService notificationService;
  private final I18n i18n;
  private final JoinGameHelper joinGameHelper;
  private final AudioService audioService;
  private final NotificationPrefs notificationPrefs;

  public void onFriendJoinedGame(PlayerBean player, GameBean game) {
    if (notificationPrefs.isFriendJoinsGameSoundEnabled()) {
      audioService.playFriendJoinsGameSound();
    }

    if (notificationPrefs.isFriendJoinsGameToastEnabled()) {
      notificationService.addNotification(new TransientNotification(
          i18n.get("friend.joinedGameNotification.title", player.getUsername(), game.getTitle()),
          i18n.get("friend.joinedGameNotification.action"),
          IdenticonUtil.createIdenticon(player.getId()),
          event -> joinGameHelper.join(game)
      ));
    }
  }
}
