package com.faforever.client.discord;

import com.faforever.client.discord.ipc.DiscordIpcListener;
import com.faforever.client.discord.ipc.DiscordUser;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.replay.ReplayRunner;
import com.faforever.client.user.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import static com.faforever.client.FafClientApplication.PROFILE_MAC;

@Slf4j
@Component
@Profile("!" + PROFILE_MAC)
public class DiscordEventHandler implements DiscordIpcListener {
  /** How long to wait for a game after connecting to the lobby, e.g. when Discord started the client. */
  private static final Duration GAME_TIMEOUT = Duration.ofSeconds(30);

  private final NotificationService notificationService;
  private final GameRunner gameRunner;
  private final ReplayRunner replayRunner;
  private final GameService gameService;
  private final ObjectMapper objectMapper;
  private final Preferences preferences;
  private final LoginService loginService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final TaskScheduler taskScheduler;

  public DiscordEventHandler(NotificationService notificationService,
                             GameRunner gameRunner, ReplayRunner replayRunner, GameService gameService,
                             ObjectMapper objectMapper,
                             Preferences preferences, LoginService loginService,
                             FxApplicationThreadExecutor fxApplicationThreadExecutor, TaskScheduler taskScheduler) {
    this.notificationService = notificationService;
    this.gameRunner = gameRunner;
    this.replayRunner = replayRunner;
    this.gameService = gameService;
    this.objectMapper = objectMapper;
    this.preferences = preferences;
    this.loginService = loginService;
    this.fxApplicationThreadExecutor = fxApplicationThreadExecutor;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public boolean onJoinRequest(DiscordUser discordUser) {
    return !preferences.isDisallowJoinsViaDiscord();
  }

  @Override
  public void onJoin(String joinSecret) {
    boolean disallowJoinsViaDiscord = preferences.isDisallowJoinsViaDiscord();
    if (disallowJoinsViaDiscord) {
      log.info("Join was requested via Discord but was rejected due to it being disabled in settings");
      return;
    }

    try {
      DiscordJoinSecret discordJoinSecret = objectMapper.readValue(joinSecret, DiscordJoinSecret.class);
      runWithGame(discordJoinSecret.gameId(), gameRunner::join);
    } catch (Exception e) {
      onError(e);
    }
  }

  @Override
  public void onSpectate(String spectateSecret) {
    try {
      DiscordSpectateSecret discordSpectateSecret = objectMapper.readValue(spectateSecret, DiscordSpectateSecret.class);
      runWithGame(discordSpectateSecret.gameId(), replayRunner::runWithLiveReplay);
    } catch (Exception e) {
      onError(e);
    }
  }

  @Override
  public void onReady(DiscordUser discordUser) {
    log.info("Discord is ready with user '{}'", discordUser.username());
  }

  private void onError(Exception e) {
    log.error("Could not join game from discord rich presence", e);
    notificationService.addImmediateErrorNotification(e, "discord.couldNotOpen");
  }

  /**
   * Runs {@code action} once the game is known. When Discord started the client, the request arrives before the client
   * is connected to the lobby and has received the game list.
   */
  private void runWithGame(int gameId, Consumer<GameInfo> action) {
    fxApplicationThreadExecutor.execute(() -> new PendingRequest(gameId, action).start());
  }

  /**
   * Waits for a game. Holds the only strong reference to the {@code game} binding, and is itself held by the connection
   * state listener and the timeout task.
   */
  private class PendingRequest implements ChangeListener<GameInfo> {
    private final int gameId;
    private final Consumer<GameInfo> action;
    private final ObservableValue<GameInfo> game;
    private final ChangeListener<ConnectionState> connectionStateListener = this::onConnectionStateChanged;
    private boolean done;

    PendingRequest(int gameId, Consumer<GameInfo> action) {
      this.gameId = gameId;
      this.action = action;
      this.game = gameService.observeByUid(gameId);
    }

    void start() {
      GameInfo current = game.getValue();
      if (current != null) {
        run(current);
        return;
      }
      log.info("Waiting for game {} requested via Discord", gameId);
      game.addListener(this);
      if (loginService.getConnectionState() == ConnectionState.CONNECTED) {
        scheduleTimeout();
      } else {
        loginService.connectionStateProperty().addListener(connectionStateListener);
      }
    }

    @Override
    public void changed(ObservableValue<? extends GameInfo> observable, GameInfo oldValue, GameInfo newValue) {
      if (newValue != null && !done) {
        finish();
        run(newValue);
      }
    }

    private void onConnectionStateChanged(ObservableValue<? extends ConnectionState> observable,
                                          ConnectionState oldValue, ConnectionState newValue) {
      if (newValue == ConnectionState.CONNECTED) {
        fxApplicationThreadExecutor.execute(() -> {
          loginService.connectionStateProperty().removeListener(connectionStateListener);
          scheduleTimeout();
        });
      }
    }

    private void scheduleTimeout() {
      taskScheduler.schedule(() -> fxApplicationThreadExecutor.execute(() -> {
        if (!done) {
          finish();
          onError(new IllegalStateException("Game " + gameId + " not found"));
        }
      }), Instant.now().plus(GAME_TIMEOUT));
    }

    private void finish() {
      done = true;
      game.removeListener(this);
      loginService.connectionStateProperty().removeListener(connectionStateListener);
    }

    private void run(GameInfo gameInfo) {
      try {
        action.accept(gameInfo);
      } catch (Exception e) {
        onError(e);
      }
    }
  }
}
