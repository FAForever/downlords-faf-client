package com.faforever.client.tournament.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.StartTabChooseController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.lobby.IsReadyRequest;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerInfo;
import com.faforever.commons.lobby.ServerMessage;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TournamentGameService implements InitializingBean {
  private final FafServerAccessor fafServerAccessor;
  private final I18n i18n;
  private final UiService uiService;
  private final NotificationService notificationService;
  private ImmediateNotification notification;


  @Override
  public void afterPropertiesSet() throws Exception {
    fafServerAccessor.addEventListener(IsReadyRequest.class, this::onReadRequest);
  }

  private void onReadRequest(IsReadyRequest isReadyRequest) {
    log.info("Tournament game is ready, asking user.");
    Stage stage = StageHolder.getStage();
    if (!stage.isFocused() || !stage.isShowing()) {
      Platform.runLater(stage::toFront);
    }
    IsReadyForGameController controller = uiService.loadFxml("theme/tournaments/is_ready_for_game.fxml");
    controller.setTimeout(isReadyRequest.getResponseTimeSeconds());
    controller.setTimedOut(() -> {
      respond(false);
      if(notification != null){
        Platform.runLater(() -> notification.dismiss());
      }
    });
    Action acceptButton = new Action(i18n.get("yes"), event -> respond(true));
    Action rejectButton = new Action(i18n.get("no"), event ->  respond(false));
    notification =
        new ImmediateNotification(i18n.get("isReady.title"), i18n.get("isReady.message", isReadyRequest.getGameName()),
            Severity.INFO, null, List.of(acceptButton, rejectButton), controller.getRoot(), false);
    notificationService.addNotification(notification);
  }

  private void respond(boolean accepted) {

  }
}
