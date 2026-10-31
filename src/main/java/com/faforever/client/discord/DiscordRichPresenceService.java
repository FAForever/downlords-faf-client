package com.faforever.client.discord;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.ipc.DiscordActivity;
import com.faforever.client.discord.ipc.DiscordActivity.Assets;
import com.faforever.client.discord.ipc.DiscordActivity.Party;
import com.faforever.client.discord.ipc.DiscordActivity.Secrets;
import com.faforever.client.discord.ipc.DiscordActivity.Timestamps;
import com.faforever.client.discord.ipc.DiscordIpcClient;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.game.GameRunner;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsWindows;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.commons.lobby.GameStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static com.faforever.client.FafClientApplication.PROFILE_MAC;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!" + PROFILE_MAC)
public class DiscordRichPresenceService implements DisposableBean, InitializingBean {
  private static final String HOSTING = "Hosting";
  private static final String WAITING = "Waiting";
  private static final String PLAYING = "Playing";

  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  private final ObjectMapper objectMapper;
  private final Preferences preferences;
  private final DiscordEventHandler discordEventHandler;
  private final GameRunner gameRunner;
  private final OperatingSystem operatingSystem;

  private DiscordIpcClient discordIpcClient;

  @Override
  public void afterPropertiesSet() throws Exception {
    String applicationId = clientProperties.getDiscord().getApplicationId();
    if (applicationId == null) {
      return;
    }
    if (operatingSystem instanceof OsWindows) {
      try {
        DiscordLaunchRegistration.registerOnWindows(applicationId);
      } catch (RuntimeException e) {
        log.warn("Could not register Discord launch command", e);
      }
    }
    discordIpcClient = DiscordIpcClient.start(applicationId, discordEventHandler);

    // This need to be change and not invalidation listeners but not quite sure why since they don't get triggered more than
    // once as invalidation listeners
    gameRunner.runningGameProperty().flatMap(GameInfo::statusProperty).subscribe(_ -> updatePlayedGame());
    gameRunner.runningGameProperty()
              .flatMap(GameInfo::allPlayersInGameProperty).subscribe(_ -> updatePlayedGame());
  }

  private void updatePlayedGame() {
    GameInfo game = gameRunner.getRunningGame();
    if (discordIpcClient == null) {
      return;
    }
    try {

      if (game == null || game.getStatus() == GameStatus.CLOSED || !playerService.isCurrentPlayerInGame(game)) {
        clearGameInfo();
        return;
      }

      log.debug("Updating discord rich presence game info with {}", game);
      String joinSecret = null;
      String spectateSecret = null;
      if (game.getStatus() == GameStatus.OPEN && !preferences.isDisallowJoinsViaDiscord()) {
        joinSecret = objectMapper.writeValueAsString(new DiscordJoinSecret(game.getId()));
      }

      if (game.getStatus() == GameStatus.PLAYING && game.getStartTime() != null && game.getStartTime()
                                                                                       .isAfter(OffsetDateTime.now()
                                                                                                              .plusSeconds(
                                                                                                                  clientProperties.getReplay()
                                                                                                                                  .getWatchDelaySeconds()))) {
        spectateSecret = objectMapper.writeValueAsString(new DiscordSpectateSecret(game.getId()));
      }

      Long startTime = null;
      if (game.getStartTime() != null) {
        startTime = game.getStartTime().toInstant().toEpochMilli();
      } else if (game.getStatus() == GameStatus.PLAYING) {
        startTime = Instant.now().toEpochMilli();
      }

      // Discord rejects the whole activity if the party size is invalid
      int partySize = game.getNumActivePlayers();
      int maxPartySize = game.getMaxPlayers();
      List<Integer> size = partySize > 0 && partySize <= maxPartySize ? List.of(partySize, maxPartySize) : null;

      discordIpcClient.setActivity(new DiscordActivity(
          getDiscordState(game),
          MessageFormat.format("{0} | {1}", game.getFeaturedMod(), game.getTitle()),
          startTime == null ? null : new Timestamps(startTime),
          new Assets(clientProperties.getDiscord().getBigImageKey(), clientProperties.getDiscord().getSmallImageKey()),
          new Party(String.valueOf(game.getId()), size),
          joinSecret == null && spectateSecret == null ? null : new Secrets(joinSecret, spectateSecret)
      ));
    } catch (Throwable throwable) {
      log.error("Error reporting game status to discord", throwable);
    }
  }

  private String getDiscordState(GameInfo game) {
    return switch (game.getStatus()) {
      case OPEN -> game.getHost().equals(playerService.getCurrentPlayer().getUsername()) ? HOSTING : WAITING;
      case PLAYING, UNKNOWN, CLOSED -> PLAYING;
    };
  }

  @Override
  public void destroy() {
    if (discordIpcClient != null) {
      discordIpcClient.close();
    }
  }

  private void clearGameInfo() {
    discordIpcClient.setActivity(null);
    log.debug("Cleared discord rich presence");
  }
}
