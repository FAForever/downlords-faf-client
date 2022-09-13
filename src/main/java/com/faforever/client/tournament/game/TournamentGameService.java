package com.faforever.client.tournament.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.IsReadyRequest;
import com.faforever.commons.lobby.MatchmakerMatchCancelledResponse;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class TournamentGameService implements InitializingBean {
  private static final int SAFETY_CLOSE_NOTIFICATION_SECONDS = 200;
  private final FafServerAccessor fafServerAccessor;
  private final I18n i18n;
  private final Timer timer = new Timer(true);
  private final UiService uiService;
  private final GameService gameService;
  private final NotificationService notificationService;
  private ImmediateNotification notification;
  private CompletableFuture<Void> matchFuture;


  @Override
  public void afterPropertiesSet() throws Exception {
    fafServerAccessor.addEventListener(IsReadyRequest.class, this::onReadRequest);
    fafServerAccessor.addEventListener(MatchmakerMatchCancelledResponse.class, this::onMatchCanceled);
    fafServerAccessor.addEventListener(GameLaunchResponse.class, this::onGameLaunch);
  }

  private void onGameLaunch(GameLaunchResponse gameLaunchResponse) {
    if(isTournamentGame(gameLaunchResponse)){
      dismissNotification();
    }
  }

  private boolean isTournamentGame(GameLaunchResponse gameLaunchResponse) {
    //TODO: implement
    return true;
  }

  private void onMatchCanceled(MatchmakerMatchCancelledResponse matchmakerMatchCancelledResponse) {
    dismissNotification();
    cancelMatch();
    notificationService.addImmediateInfoNotification(
        "isReady.matchCanceled"
    );
  }

  private void onReadRequest(IsReadyRequest isReadyRequest) {
    log.info("Tournament game is ready, asking user.");
    if(notification != null){
      log.warn("Tournament ready request ignored because tournament is already in progress.");
      respondToReadyRequest(isReadyRequest.getRequestId(), isReadyRequest);
      return;
    }
    bringStageToFront();
    final var controller = initializeIsReadyController(isReadyRequest);
    notification =
        new ImmediateNotification(i18n.get("isReady.title"), i18n.get("isReady.message", isReadyRequest.getGameName()),
            Severity.INFO, null, List.of(), controller.getRoot(), false);
    notificationService.addNotification(notification);
    safetyCloseMechanism();
  }

  private void safetyCloseMechanism() {
    final var currentNotification = notification;
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        if(notification != currentNotification){
          return;
        }
        JavaFxUtil.runLater(()-> dismissNotification());
      }
    }, SAFETY_CLOSE_NOTIFICATION_SECONDS * 1000);
  }

  private void dismissNotification() {
    if(notification == null){
      return;
    }
    notification.dismiss();
    notification = null;
  }

  @NotNull
  private IsReadyForGameController initializeIsReadyController(IsReadyRequest isReadyRequest) {
    IsReadyForGameController controller = uiService.loadFxml("theme/tournaments/is_ready_for_game.fxml");
    controller.setTimeout(isReadyRequest.getResponseTimeSeconds());
    controller.setIsReadyCallBack(() -> respondToReadyRequest(isReadyRequest.getRequestId(), isReadyRequest));
    controller.setDismissCallBack(() -> JavaFxUtil.runLater(this::dismissNotification));
    return controller;
  }

  private static void bringStageToFront() {
    Stage stage = StageHolder.getStage();
    if (!stage.isFocused() || !stage.isShowing()) {
      JavaFxUtil.runLater(stage::toFront);
    }
  }

  private void respondToReadyRequest(String requestId, IsReadyRequest isReadyRequest) {
    //TODO: Quit tmm queues
    matchFuture = gameService.startListeningToTournamentGame(isReadyRequest.getFeaturedMod());
    try{
      fafServerAccessor.sendIsReady(requestId);
    }catch (Exception e){
      dismissNotification();
      cancelMatch();
      notificationService.addImmediateErrorNotification(e, "isReady.readyUpFailed");
    }
  }

  private void cancelMatch(){
    if (matchFuture != null) {
      matchFuture.cancel(false);
    }
  }
}
