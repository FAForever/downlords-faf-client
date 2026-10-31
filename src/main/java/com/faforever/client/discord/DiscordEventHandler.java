package com.faforever.client.discord;

import com.faforever.client.discord.ipc.DiscordIpcListener;
import com.faforever.client.discord.ipc.DiscordUser;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.replay.ReplayRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.faforever.client.FafClientApplication.PROFILE_MAC;

@Slf4j
@Component
@Profile("!" + PROFILE_MAC)
public class DiscordEventHandler implements DiscordIpcListener {
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
      GameInfo gameInfo = gameService.getByUid(discordJoinSecret.gameId()).orElseThrow();
      gameRunner.join(gameInfo);
    } catch (Exception e) {
      log.error("Could not join game from discord rich presence", e);
      notificationService.addImmediateErrorNotification(e, "discord.couldNotOpen");
    }
  }

  @Override
  public void onSpectate(String spectateSecret) {
    try {
      DiscordSpectateSecret discordSpectateSecret = objectMapper.readValue(spectateSecret, DiscordSpectateSecret.class);
      GameInfo game = gameService.getByUid(discordSpectateSecret.gameId()).orElseThrow();
      replayRunner.runWithLiveReplay(game);
    } catch (Exception e) {
      log.error("Could not join game from discord rich presence", e);
      notificationService.addImmediateErrorNotification(e, "discord.couldNotOpen");
    }
  }

  @Override
  public void onReady(DiscordUser discordUser) {
    log.info("Discord is ready with user '{}'", discordUser.username());
  }
}
