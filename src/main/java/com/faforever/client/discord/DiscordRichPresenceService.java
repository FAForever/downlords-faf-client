package com.faforever.client.discord;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.Game;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Service
public class DiscordRichPresenceService {
  private static final String HOSTING = "Hosting";
  private static final String WAITING = "Waiting";
  private static final String PLAYING = "Playing";
  /**
   * It is suggested as per the library's GitHub page to look for callbacks every 5 seconds.
   * @see <a href="https://github.com/Vatuu/discord-rpc">Projects GitHubPage</a>
   */
  private static final int INITIAL_DELAY_FOR_CALLBACK_MILLIS = 5000;
  private static final int PERIOD_FOR_CALLBACK_MILLIS = 5000;

  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  private final PreferencesService preferencesService;
  private final Timer timer;


  public DiscordRichPresenceService(PlayerService playerService, ClientDiscordEventHandler discordEventHandler,
                                    ClientProperties clientProperties, PreferencesService preferencesService) {
    this.playerService = playerService;
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
    this.timer = new Timer(true);
    String applicationId = clientProperties.getDiscord().getApplicationId();
    if (applicationId == null) {
      return;
    }
    try {
      DiscordRPC.discordInitialize(applicationId, discordEventHandler, true);
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          DiscordRPC.discordRunCallbacks();
        }
      }, INITIAL_DELAY_FOR_CALLBACK_MILLIS, PERIOD_FOR_CALLBACK_MILLIS);
    } catch (Throwable t) {
      //TODO: report to bugsnap
      log.error("Error in discord init", t);
    }
  }

  public void updatePlayedGameTo(Game game, int currentPlayerId) {
    String applicationId = clientProperties.getDiscord().getApplicationId();
    if (applicationId == null) {
      return;
    }
    try {
      if (game.getStatus() == GameStatus.CLOSED) {
        DiscordRPC.discordClearPresence();
      }
      DiscordRichPresence.Builder discordRichPresence = new DiscordRichPresence.Builder(getDiscordState(game));
      discordRichPresence.setDetails(MessageFormat.format("{0} | {1}", game.getFeaturedMod(), game.getTitle()));
      discordRichPresence.setParty(String.valueOf(game.getId()), game.getNumPlayers(), game.getMaxPlayers());
      discordRichPresence.setSmallImage(clientProperties.getDiscord().getSmallImageKey(), "");
      discordRichPresence.setBigImage(clientProperties.getDiscord().getBigImageKey(), "");
      String joinSecret = null;
      String spectateSecret = null;
      if (game.getStatus() == GameStatus.OPEN && !preferencesService.getPreferences().isDisallowJoinsViaDiscord()) {
        joinSecret = new Gson().toJson(new DiscordJoinSecret(game.getId()));
      }

      if (game.getStatus() == GameStatus.PLAYING && game.getStartTime().isAfter(Instant.now().plus(5,ChronoUnit.MINUTES))) {
        spectateSecret = new Gson().toJson(new DiscordSpectateSecret(game.getId(),currentPlayerId));
      }

      discordRichPresence.setSecrets(joinSecret, spectateSecret);

      DiscordRichPresence update = discordRichPresence.build();
      if (game.getStartTime() != null) {
        //Sadly, this can not be set via the builder. I created an issue in the library's repo
        // TODO set via builder when https://github.com/Vatuu/discord-rpc/issues/18 has been fixed
        update.startTimestamp = game.getStartTime().toEpochMilli();
      }
      DiscordRPC.discordUpdatePresence(update);
    } catch (Throwable throwable) {
      //TODO: report to bugsnap
      log.error("Error reporting game status to discord", throwable);
    }
  }

  private String getDiscordState(Game game) {
    //I want no internationalisation in here as it should always be English
    switch (game.getStatus()) {
      case OPEN:
        boolean isHost = game.getHost().equals(playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player must have been set")).getUsername());
        return isHost ? HOSTING : WAITING;
      default:
        return PLAYING;
    }
  }

  @PreDestroy
  public void onDestroy() {
    DiscordRPC.discordShutdown();
    timer.cancel();
  }
}
