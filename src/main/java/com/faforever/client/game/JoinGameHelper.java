package com.faforever.client.game;

import com.faforever.client.discord.DiscordJoinEvent;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateErrorNotification;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.util.RatingUtil;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;

@Component
@Slf4j
public class JoinGameHelper {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final I18n i18n;
  private final PlayerService playerService;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final ReportingService reportingService;
  private final UiService uiService;
  private final EventBus eventBus;

  public JoinGameHelper(I18n i18n, PlayerService playerService, GameService gameService, PreferencesService preferencesService, NotificationService notificationService, ReportingService reportingService, UiService uiService, EventBus eventBus) {
    this.i18n = i18n;
    this.playerService = playerService;
    this.gameService = gameService;
    this.preferencesService = preferencesService;
    this.notificationService = notificationService;
    this.reportingService = reportingService;
    this.uiService = uiService;
    this.eventBus = eventBus;
  }

  public void join(Game game) {
    this.join(game, null, false);
  }

  public void join(Game game, String password, boolean ignoreRating) {
    Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has not been set"));
    int playerRating = RatingUtil.getRoundedGlobalRating(currentPlayer);

    if (!preferencesService.isGamePathValid()) {
      CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
      eventBus.post(new GameDirectoryChooseEvent(gameDirectoryFuture));
      gameDirectoryFuture.thenAccept(path -> Optional.ofNullable(path).ifPresent(path1 -> join(game, password, ignoreRating)));
      return;
    }

    if (!ignoreRating && (playerRating < game.getMinRating() || playerRating > game.getMaxRating())) {
      showRatingOutOfBoundsConfirmation(playerRating, game, password);
      return;
    }

    if (game.isPasswordProtected() && password == null) {
      EnterPasswordController enterPasswordController = uiService.loadFxml("theme/enter_password.fxml");
      enterPasswordController.setOnPasswordEnteredListener(this::join);
      enterPasswordController.setGame(game);
      enterPasswordController.setIgnoreRating(ignoreRating);
      enterPasswordController.showPasswordDialog(StageHolder.getStage());
    } else {
      gameService.joinGame(game, password)
          .exceptionally(throwable -> {
            logger.warn("Game could not be joined", throwable);
            notificationService.addNotification(new ImmediateErrorNotification(
                i18n.get("errorTitle"),
                i18n.get("games.couldNotJoin"),
                throwable,
                i18n, reportingService
            ));
            return null;
          });
    }
  }

  private void showRatingOutOfBoundsConfirmation(int playerRating, Game game, String password) {
    notificationService.addNotification(new ImmediateNotification(
        i18n.get("game.joinGameRatingConfirmation.title"),
        i18n.get("game.joinGameRatingConfirmation.text", game.getMinRating(), game.getMaxRating(), playerRating),
        Severity.INFO,
        asList(
            new Action(i18n.get("game.join"), event -> this.join(game, password, true)),
            new Action(i18n.get("game.cancel"))
        )
    ));
  }

  public void join(int gameId) {
    join(gameService.getByUid(gameId));
  }

  @EventListener
  public void onDiscordGameJoinEvent(DiscordJoinEvent discordJoinEvent) {
    Integer gameId = discordJoinEvent.getGameId();
    boolean disallowJoinsViaDiscord = preferencesService.getPreferences().isDisallowJoinsViaDiscord();
    if (disallowJoinsViaDiscord) {
      log.debug("Join was requested via Discord but was rejected due to it being disabled in settings");
      return;
    }
    Platform.runLater(() -> join(gameId));
  }
}
