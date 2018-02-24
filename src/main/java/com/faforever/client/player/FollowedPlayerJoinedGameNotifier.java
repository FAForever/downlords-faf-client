package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.game.Game;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.event.FollowedPlayerJoinedGameEvent;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

import static com.github.nocatch.NoCatch.noCatch;

/**
 * Displays a notification whenever a friend joins a preferences (if enabled in settings).
 */
@Component
public class FollowedPlayerJoinedGameNotifier {

  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final JoinGameHelper joinGameHelper;
  private final AudioService audioService;

  @Inject
  public FollowedPlayerJoinedGameNotifier(NotificationService notificationService, I18n i18n, EventBus eventBus,
                                          JoinGameHelper joinGameHelper,
                                          AudioService audioService) {
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.joinGameHelper = joinGameHelper;
    this.audioService = audioService;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onFollowedJoinedGame(FollowedPlayerJoinedGameEvent event) {
    Game game = event.getGame();
    Player player = event.getPlayer();

    audioService.playFriendJoinsGameSound();

    CompletableFuture<Boolean> future = new CompletableFuture<>();

    notificationService.addNotification(new TransientNotification(
        i18n.get("followed.joinedGameNotification.title", player.getUsername(), game.getTitle()),
        i18n.get("followed.joinedGameNotification.action"),
        IdenticonUtil.createIdenticon(player.getId()),
        event1 -> future.cancel(true))
    );

    new Thread(() -> noCatch(() -> {
      Thread.sleep(3000);
      future.complete(game.getStatus() == GameStatus.OPEN);
    })).start();

    future.thenAccept(status -> {
      if (status) {
        joinGameHelper.join(game);
      }
    });
  }
}
