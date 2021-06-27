package com.faforever.client.discord;


import com.faforever.client.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  public DiscordEventHandler(ApplicationEventPublisher applicationEventPublisher, NotificationService notificationService, ObjectMapper objectMapper) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
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
    try {
      DiscordJoinSecret discordJoinSecret = objectMapper.readValue(joinSecret, DiscordJoinSecret.class);
      applicationEventPublisher.publishEvent(new DiscordJoinEvent(discordJoinSecret.getGameId()));
    } catch (Exception e) {
      log.error("Could not join game from discord rich presence", e);
      notificationService.addImmediateErrorNotification(e, "discord.couldNotOpen");
    }
  }

  private void onSpectate(String spectateSecret) {
    try {
      DiscordSpectateSecret discordSpectateSecret = objectMapper.readValue(spectateSecret, DiscordSpectateSecret.class);
      applicationEventPublisher.publishEvent(new DiscordSpectateEvent(discordSpectateSecret.getGameId()));
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
