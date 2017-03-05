package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.util.RatingUtil;
import com.google.common.eventbus.EventBus;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.ERROR;
import static java.util.Arrays.asList;

@Component
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

  private Node parentNode;

  @Inject
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

  public void setParentNode(Node parentNode) {
    this.parentNode = parentNode;
  }

  public void join(Game game) {
    this.join(game, null, false);
  }

  public void join(Game game, String password, boolean ignoreRating) {
    Objects.requireNonNull(parentNode, "parentNode has not been set");

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

    if (game.getPasswordProtected() && password == null) {
      EnterPasswordController enterPasswordController = uiService.loadFxml("theme/enter_password.fxml");
      enterPasswordController.setOnPasswordEnteredListener(this::join);
      enterPasswordController.setGame(game);
      enterPasswordController.setIgnoreRating(ignoreRating);
      enterPasswordController.showPasswordDialog(parentNode.getScene().getWindow());
    } else {
      gameService.joinGame(game, password)
          .exceptionally(throwable -> {
            logger.warn("Game could not be joined", throwable);
            notificationService.addNotification(
                new ImmediateNotification(
                    i18n.get("errorTitle"),
                    i18n.get("games.couldNotJoin"),
                    ERROR,
                    throwable,
                    asList(new DismissAction(i18n), new ReportAction(i18n, reportingService, throwable))));
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
}
