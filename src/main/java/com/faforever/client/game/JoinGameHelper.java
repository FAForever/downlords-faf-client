package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import com.faforever.client.util.RatingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class JoinGameHelper {

  private final I18n i18n;
  private final PlayerService playerService;
  private final GameService gameService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final UiService uiService;
  private final Preferences preferences;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final GameDirectoryRequiredHandler gameDirectoryRequiredHandler;

  public void join(GameBean game) {
    this.join(game, null, false);
  }

  public void join(GameBean game, String password, boolean ignoreRating) {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    int playerRating = RatingUtil.getRoundedLeaderboardRating(currentPlayer, game.getLeaderboard());

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
      gameDirectoryRequiredHandler.onChooseGameDirectory(gameDirectoryFuture);
      gameDirectoryFuture.thenAccept(path -> Optional.ofNullable(path)
          .ifPresent(path1 -> join(game, password, ignoreRating)));
      return;
    }

    boolean minRatingViolated = game.getRatingMin() != null && playerRating < game.getRatingMin();
    boolean maxRatingViolated = game.getRatingMax() != null && playerRating > game.getRatingMax();

    if (!ignoreRating && (minRatingViolated || maxRatingViolated)) {
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
            log.error("Game could not be joined", throwable);
            notificationService.addImmediateErrorNotification(throwable, "games.couldNotJoin");
            return null;
          });
    }
  }

  private void showRatingOutOfBoundsConfirmation(int playerRating, GameBean game, String password) {
    notificationService.addNotification(new ImmediateNotification(
        i18n.get("game.joinGameRatingConfirmation.title"),
        i18n.get("game.joinGameRatingConfirmation.text", game.getRatingMin(), game.getRatingMax(), playerRating),
        Severity.INFO,
        List.of(
            new Action(i18n.get("game.join"), event -> this.join(game, password, true)),
            new Action(i18n.get("game.cancel"))
        )
    ));
  }

  public void join(int gameId) {
    join(gameService.getByUid(gameId));
  }

  public void onDiscordGameJoinEvent(Integer gameId) {
    boolean disallowJoinsViaDiscord = preferences.isDisallowJoinsViaDiscord();
    if (disallowJoinsViaDiscord) {
      log.info("Join was requested via Discord but was rejected due to it being disabled in settings");
      return;
    }
    fxApplicationThreadExecutor.execute(() -> join(gameId));
  }
}
