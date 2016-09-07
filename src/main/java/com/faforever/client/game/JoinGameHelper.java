package com.faforever.client.game;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.RatingUtil;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

import static com.faforever.client.notification.Severity.ERROR;
import static java.util.Arrays.asList;

public class JoinGameHelper {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Resource
  ApplicationContext applicationContext;
  @Resource
  I18n i18n;
  @Resource
  PlayerService playerService;
  @Resource
  GameService gameService;
  @Resource
  MapService mapService;
  @Resource
  CreateGameController createGameController;
  @Resource
  EnterPasswordController enterPasswordController;
  @Resource
  PreferencesService preferencesService;
  @Resource
  NotificationService notificationService;
  @Resource
  ReportingService reportingService;
  private Node parentNode;

  public void setParentNode(Node parentNode) {
    this.parentNode = parentNode;
  }

  @PostConstruct
  void postConstruct() {
    enterPasswordController.setOnPasswordEnteredListener(this::join);
  }

  public void join(GameInfoBean gameInfoBean) {
    this.join(gameInfoBean, null, false);
  }

  public void join(GameInfoBean gameInfoBean, String password, boolean ignoreRating) {
    Objects.requireNonNull(parentNode, "parentNode has not been set");

    PlayerInfoBean currentPlayer = playerService.getCurrentPlayer();
    int playerRating = RatingUtil.getRoundedGlobalRating(currentPlayer);

    if (!preferencesService.isGamePathValid()) {
      preferencesService.letUserChooseGameDirectory()
          .thenAccept(path -> {
            if (path != null) {
              join(gameInfoBean, password, ignoreRating);
            }
          });
      return;
    }

    if (!ignoreRating && (playerRating < gameInfoBean.getMinRating() || playerRating > gameInfoBean.getMaxRating())) {
      showRatingOutOfBoundsConfirmation(playerRating, gameInfoBean, password);
      return;
    }

    if (gameInfoBean.getPasswordProtected() && password == null) {
      enterPasswordController.setGameInfoBean(gameInfoBean);
      enterPasswordController.setIgnoreRating(ignoreRating);
      enterPasswordController.showPasswordDialog(parentNode.getScene().getWindow());
    } else {
      gameService.joinGame(gameInfoBean, password)
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

  private void showRatingOutOfBoundsConfirmation(int playerRating, GameInfoBean gameInfoBean, String password) {
    notificationService.addNotification(new ImmediateNotification(
        i18n.get("game.joinGameRatingConfirmation.title"),
        i18n.get("game.joinGameRatingConfirmation.text", gameInfoBean.getMinRating(), gameInfoBean.getMaxRating(), playerRating),
        Severity.INFO,
        asList(
            new Action(i18n.get("game.join"), event -> this.join(gameInfoBean, password, true)),
            new Action(i18n.get("game.cancel"))
        )
    ));
  }

  public void join(int gameId) {
    join(gameService.getByUid(gameId));
  }
}
