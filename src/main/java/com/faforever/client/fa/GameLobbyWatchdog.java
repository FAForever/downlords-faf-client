package com.faforever.client.fa;

import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

/**
 * Once the game has entered its lobby it waits for the server to tell it to either host or join a game. If the server
 * has given up on the game in the meantime, e.g. because the game took too long to start, that instruction never
 * arrives and the game shows "Connecting..." forever. When that happens this watchdog first tries to join the game
 * again, and tells the user if that is not possible or does not help either.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameLobbyWatchdog implements InitializingBean {

  @VisibleForTesting
  static final Duration SERVER_RESPONSE_TIMEOUT = Duration.ofSeconds(10);

  private final FafServerAccessor fafServerAccessor;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final GameRunner gameRunner;
  private final TaskScheduler taskScheduler;

  private ScheduledFuture<?> pendingTimeout;

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(HostGameGpgCommand.class)
                     .doOnNext(_ -> onServerResponded())
                     .doOnError(throwable -> log.warn("Error processing host game command", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(JoinGameGpgCommand.class)
                     .doOnNext(_ -> onServerResponded())
                     .doOnError(throwable -> log.warn("Error processing join game command", throwable))
                     .retry()
                     .subscribe();
  }

  public synchronized void onGameEnteredLobby() {
    cancelPendingTimeout();
    pendingTimeout = taskScheduler.schedule(this::onServerDidNotRespond, Instant.now().plus(SERVER_RESPONSE_TIMEOUT));
  }

  @VisibleForTesting
  synchronized void onServerResponded() {
    cancelPendingTimeout();
  }

  private void cancelPendingTimeout() {
    if (pendingTimeout != null) {
      pendingTimeout.cancel(false);
      pendingTimeout = null;
    }
  }

  private void onServerDidNotRespond() {
    synchronized (this) {
      pendingTimeout = null;
    }

    if (!gameRunner.isRunning()) {
      return;
    }

    log.warn("Game entered the lobby but the server sent neither a host nor a join command within {} seconds",
             SERVER_RESPONSE_TIMEOUT.toSeconds());

    Optional<CompletableFuture<Void>> rejoin = gameRunner.rejoinRunningGame();
    if (rejoin.isPresent()) {
      rejoin.get().whenComplete((_, throwable) -> {
        if (throwable != null) {
          log.warn("Could not join the game again", throwable);
          notifyUser();
        } else {
          onGameEnteredLobby();
        }
      });
      return;
    }

    notifyUser();
  }

  private void notifyUser() {
    notificationService.addNotification(
        new ImmediateNotification(i18n.get("game.lobbyTimeout.title"), i18n.get("game.lobbyTimeout.message"),
                                  Severity.WARN,
                                  List.of(new Action(i18n.get("game.lobbyTimeout.close"), gameRunner::killGame),
                                          new DismissAction(i18n))));
  }
}
