package com.faforever.client.leaderboard;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenGlobalLeaderboardEvent;
import com.faforever.client.main.event.OpenLadder1v1LeaderboardEvent;
import com.google.common.eventbus.EventBus;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LeaderboardsController extends AbstractViewController<Node> {
  private final EventBus eventBus;

  public TabPane leaderboardRoot;
  public LeaderboardController ladder1v1LeaderboardController;
  public LeaderboardController globalLeaderboardController;
  public Tab ladder1v1LeaderboardTab;
  public Tab globalLeaderboardTab;

  private boolean isHandlingEvent;
  private AbstractViewController<?> lastTabController;
  private Tab lastTab;

  public LeaderboardsController(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Node getRoot() {
    return leaderboardRoot;
  }

  @Override
  public void initialize() {
    lastTab = ladder1v1LeaderboardTab;
    lastTabController = ladder1v1LeaderboardController;
    ladder1v1LeaderboardController.setRatingType(KnownFeaturedMod.LADDER_1V1);
    globalLeaderboardController.setRatingType(KnownFeaturedMod.FAF);

    leaderboardRoot.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newValue == ladder1v1LeaderboardTab) {
        eventBus.post(new OpenLadder1v1LeaderboardEvent());
      } else if (newValue == globalLeaderboardTab) {
        eventBus.post(new OpenGlobalLeaderboardEvent());
      }
      // TODO implement other tabs
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (navigateEvent instanceof OpenLadder1v1LeaderboardEvent) {
        lastTab = ladder1v1LeaderboardTab;
        lastTabController = ladder1v1LeaderboardController;
      } else if (navigateEvent instanceof OpenGlobalLeaderboardEvent) {
        lastTab = globalLeaderboardTab;
        lastTabController = globalLeaderboardController;
      }
      leaderboardRoot.getSelectionModel().select(lastTab);
      lastTabController.display(navigateEvent);
    } finally {
      isHandlingEvent = false;
    }
  }
}
