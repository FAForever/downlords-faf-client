package com.faforever.client.leaderboard;

import com.faforever.client.domain.api.League;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
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
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;


@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LeaderboardsController extends NodeController<Node> {
  private final I18n i18n;
  private final LeaderboardService leaderboardService;
  private final NotificationService notificationService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final NavigationHandler navigationHandler;

  public VBox leaderboardRoot;
  public HBox navigationBox;
  public StackPane contentPane;
  public LeaderboardController leaderboardController;

  private final ToggleGroup navigation = new ToggleGroup();
  private final Map<Toggle, League> toggleLeagueMap = new HashMap<>();

  @Override
  public Node getRoot() {
    return leaderboardRoot;
  }

  @Override
  protected void onInitialize() {
    navigation.selectedToggleProperty().when(showing).subscribe((oldToggle, newToggle) -> {
      if (newToggle != null) {
        setLeague(toggleLeagueMap.get(newToggle));
      } else {
        navigation.selectToggle(oldToggle);
      }
    });

    leaderboardService.getLeagues()
                      .map(league -> {
                        String buttonText = i18n.getOrDefault(league.technicalName(), String.format("leaderboard.%s",
                                                                            league.technicalName()));
                        ToggleButton toggleButton = new ToggleButton(buttonText);
                        toggleButton.setToggleGroup(navigation);
                        toggleButton.getStyleClass().add("main-navigation-button");
                        toggleLeagueMap.put(toggleButton, league);
                        return toggleButton;
                      })
                      .switchIfEmpty(Mono.error(new IllegalStateException("No leagues loaded")))
                      .collectList()
                      .doOnNext(leagueButtons -> {
                        League lastLeagueTab = navigationHandler.getLastLeagueTab();
                        Toggle startingToggle = toggleLeagueMap.entrySet()
                                                               .stream()
                                                               .filter(entry -> Objects.equals(lastLeagueTab,
                                                                                               entry.getValue()))
                                                               .findFirst()
                                                               .map(Entry::getKey)
                                                               .orElse(leagueButtons.getFirst());
                        navigation.selectToggle(startingToggle);
                      })
                      .publishOn(fxApplicationThreadExecutor.asScheduler())
                      .subscribe(buttons -> navigationBox.getChildren().setAll(buttons), throwable -> {
                        log.error("Error while loading leagues", throwable);
                        notificationService.addImmediateErrorNotification(throwable,
                                                                          "leaderboard.failedToLoadLeaderboards");
                      });
  }

  private void setLeague(League league) {
    navigationHandler.setLastLeagueTab(league);
    leaderboardService.getLatestSeason(league)
                      .publishOn(fxApplicationThreadExecutor.asScheduler())
                      .subscribe(leaderboardController::setLeagueSeason, throwable -> {
                        log.error("Error while loading seasons", throwable);
                        notificationService.addImmediateErrorNotification(throwable,
                                                                          "leaderboard.failedToLoadLeaderboards");
                      });
  }
}
