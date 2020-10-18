package com.faforever.client.discord;


import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRPC.DiscordReply;
import net.arikia.dev.drpc.DiscordUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DiscordEventHandler extends DiscordEventHandlers {
  private final ApplicationEventPublisher applicationEventPublisher;
  private final NotificationService notificationService;

  public DiscordEventHandler(ApplicationEventPublisher applicationEventPublisher, NotificationService notificationService) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.notificationService = notificationService;
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
    DiscordJoinSecret discordJoinSecret = new Gson().fromJson(joinSecret, DiscordJoinSecret.class);
    try {
      applicationEventPublisher.publishEvent(new DiscordJoinEvent(discordJoinSecret.getGameId()));
    } catch (Exception e) {
      log.error("Could not join game from discord rich presence", e);
      notificationService.addImmediateErrorNotification(e, "game.couldNotJoin", discordJoinSecret.getGameId());
    }
  }

  private void onSpectate(String spectateSecret) {
    DiscordSpectateSecret discordSpectateSecret = new Gson().fromJson(spectateSecret, DiscordSpectateSecret.class);
    try {
      applicationEventPublisher.publishEvent(new DiscordSpectateEvent(discordSpectateSecret.getGameId()));
    } catch (Exception e) {
      log.error("Could not join game from discord rich presence", e);
      notificationService.addImmediateErrorNotification(e, "replay.couldNotOpen", discordSpectateSecret.getGameId());
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
