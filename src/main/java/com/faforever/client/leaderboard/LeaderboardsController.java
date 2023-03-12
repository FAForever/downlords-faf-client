package com.faforever.client.leaderboard;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLeaderboardEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LeaderboardsController extends AbstractViewController<Node> {
  private final EventBus eventBus;
  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final UiService uiService;

  public TabPane leaderboardRoot;

  private boolean isHandlingEvent;
  @VisibleForTesting
  protected final List<LeaderboardController> controllers = new ArrayList<>();
  private Tab lastTab;

  @Override
  public Node getRoot() {
    return leaderboardRoot;
  }

  @Override
  public void initialize() {
    leaderboardService.getLeagues().thenAccept(leagues -> {
      leagues.forEach(league ->
          leaderboardService.getLatestSeason(league).thenAccept(season -> {
            LeaderboardController controller = uiService.loadFxml("theme/leaderboard/leaderboard.fxml");
            controller.setSeason(season);
            controller.getRoot().setText(i18n.getOrDefault(league.getTechnicalName(), String.format("leaderboard.%s", league.getTechnicalName())));
            controller.getRoot().setUserData(league.getId());
            JavaFxUtil.runLater(() -> leaderboardRoot.getTabs().add(controller.getRoot()));
            controllers.add(controller);
          }).thenRun(() -> JavaFxUtil.runLater(() -> {
            leaderboardRoot.getTabs().sort(Comparator.comparing(tab -> (int) tab.getUserData()));
            leaderboardRoot.getSelectionModel().select(0);
            lastTab = leaderboardRoot.getTabs().get(0);
          })).exceptionally(throwable -> {
            log.error("Error while loading seasons", throwable);
            notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoadLeaderboards");
            return null;
          })
      );
      if (leagues.isEmpty()) {
        log.warn("Api returned no leagues");
        notificationService.addImmediateWarnNotification("leaderboard.noLeaderboards");
      }
    }).exceptionally(throwable -> {
      log.error("Error while loading leagues", throwable);
      notificationService.addImmediateErrorNotification(throwable, "leaderboard.failedToLoadLeaderboards");
      return null;
    });

    leaderboardRoot.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }
      eventBus.post(new OpenLeaderboardEvent(newValue));
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (navigateEvent instanceof OpenLeaderboardEvent openLeaderboardEvent) {
        controllers.forEach(controller -> {
          if (controller.getRoot().equals(openLeaderboardEvent.getLeagueTab())) {
            lastTab = controller.getRoot();
          }
        });
      }
      leaderboardRoot.getSelectionModel().select(lastTab);
    } finally {
      isHandlingEvent = false;
    }
  }
}
