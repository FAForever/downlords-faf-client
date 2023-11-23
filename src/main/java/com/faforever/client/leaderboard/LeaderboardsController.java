package com.faforever.client.leaderboard;

import com.faforever.client.domain.LeagueBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LeaderboardsController extends NodeController<Node> {
  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final NavigationHandler navigationHandler;

  public VBox leaderboardRoot;
  public HBox navigationBox;
  public StackPane contentPane;

  private final ToggleGroup navigation = new ToggleGroup();
  private final Map<LeagueBean, ToggleButton> buttonLeagueMap = new HashMap<>();

  @Override
  public Node getRoot() {
    return leaderboardRoot;
  }

  @Override
  protected void onInitialize() {
    leaderboardService.getLeagues()
                      .thenApply(leagues -> {
                        if (leagues.isEmpty()) {
                          log.warn("Api returned no leagues");
                          notificationService.addImmediateWarnNotification("leaderboard.noLeaderboards");
                          return List.<ToggleButton>of();
                        }

                        List<ToggleButton> leagueButtons = leagues.stream().map(league -> {
                          String buttonText = i18n.getOrDefault(league.getTechnicalName(),
                                                                String.format("leaderboard.%s",
                                                                              league.getTechnicalName()));
                          ToggleButton toggleButton = new ToggleButton(buttonText);
                          toggleButton.setToggleGroup(navigation);
                          toggleButton.getStyleClass().add("main-navigation-button");

                          toggleButton.setOnAction(event -> loadLeague(league));
                          buttonLeagueMap.put(league, toggleButton);
                          return toggleButton;
                        }).toList();

                        LeagueBean lastLeagueTab = navigationHandler.getLastLeagueTab();
                        loadLeague(lastLeagueTab == null ? leagues.get(0) : lastLeagueTab);

                        return leagueButtons;
                      })
                      .thenAcceptAsync(buttons -> navigationBox.getChildren().setAll(buttons),
                                       fxApplicationThreadExecutor)
                      .exceptionally(throwable -> {
                        log.error("Error while loading leagues", throwable);
                        notificationService.addImmediateErrorNotification(throwable,
                                                                          "leaderboard.failedToLoadLeaderboards");
                        return null;
                      });
  }

  private void loadLeague(LeagueBean league) {
    ToggleButton toggleButton = buttonLeagueMap.get(league);
    if (toggleButton != null) {
      fxApplicationThreadExecutor.execute(() -> toggleButton.setSelected(true));
    }
    navigationHandler.setLastLeagueTab(league);
    leaderboardService.getLatestSeason(league)
                      .thenApply(season -> {
                        LeaderboardController controller = uiService.loadFxml("theme/leaderboard/leaderboard.fxml");
                        controller.setSeason(season);
                        return controller.getRoot();
                      })
                      .thenAcceptAsync(tab -> contentPane.getChildren().setAll(tab), fxApplicationThreadExecutor)
                      .exceptionally(throwable -> {
                        log.error("Error while loading seasons", throwable);
                        notificationService.addImmediateErrorNotification(throwable,
                                                                          "leaderboard.failedToLoadLeaderboards");
                        return null;
                      });
  }
}
