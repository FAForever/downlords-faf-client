package com.faforever.client.discord;

import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.replay.ReplayRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRPC.DiscordReply;
import net.arikia.dev.drpc.DiscordUser;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DiscordEventHandler extends DiscordEventHandlers {
  private final NotificationService notificationService;
  private final GameRunner gameRunner;
  private final ReplayRunner replayRunner;
  private final GameService gameService;
  private final ObjectMapper objectMapper;
  private final Preferences preferences;

  public DiscordEventHandler(NotificationService notificationService,
                             GameRunner gameRunner, ReplayRunner replayRunner, GameService gameService,
                             ObjectMapper objectMapper,
                             Preferences preferences) {
    this.notificationService = notificationService;
    this.gameRunner = gameRunner;
    this.replayRunner = replayRunner;
    this.gameService = gameService;
    this.objectMapper = objectMapper;
    this.preferences = preferences;
    ready = this::onDiscordReady;
    disconnected = this::onDisconnected;
    errored = this::onError;
    spectateGame = this::onSpectate;
    joinGame = this::onJoinGame;
    joinRequest = this::onJoinRequest;
  }

  private void onJoinRequest(DiscordUser discordUser) {
    DiscordRPC.discordRespond(discordUser.userId, DiscordReply.YES);
  }

  private void onJoinGame(String joinSecret) {
    boolean disallowJoinsViaDiscord = preferences.isDisallowJoinsViaDiscord();
    if (disallowJoinsViaDiscord) {
      log.info("Join was requested via Discord but was rejected due to it being disabled in settings");
      return;
    }

    try {
      DiscordJoinSecret discordJoinSecret = objectMapper.readValue(joinSecret, DiscordJoinSecret.class);
      GameInfo gameInfo = gameService.getByUid(discordJoinSecret.gameId()).orElseThrow();
      gameRunner.join(gameInfo);
    } catch (Exception e) {
      log.error("Could not join game from discord rich presence", e);
      notificationService.addImmediateErrorNotification(e, "discord.couldNotOpen");
    }
  }

  private void onSpectate(String spectateSecret) {
    try {
      DiscordSpectateSecret discordSpectateSecret = objectMapper.readValue(spectateSecret, DiscordSpectateSecret.class);
      GameInfo game = gameService.getByUid(discordSpectateSecret.gameId()).orElseThrow();
      replayRunner.runWithLiveReplay(game);
    } catch (Exception e) {
      log.error("Could not join game from discord rich presence", e);
      notificationService.addImmediateErrorNotification(e, "discord.couldNotOpen");
    }
  }

  private void onError(int errorCode, String message) {
    log.error("Discord error with code '{}' and message '{}'", errorCode, message);
  }

  private void onDisconnected(int code, String message) {
    log.info("Discord disconnected with code '{}' and message '{}'", code, message);
  }

  private void onDiscordReady(DiscordUser discordUser) {
    log.info("Discord is ready with user '{}'", discordUser.username);
  }
}
