package com.faforever.client.play;

import com.faforever.client.coop.CoopController;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.CustomGamesController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenCoopEvent;
import com.faforever.client.main.event.OpenCustomGamesEvent;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.teammatchmaking.TeamMatchmakingController;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlayController extends AbstractViewController<Node> {

  private final NavigationHandler navigationHandler;
  public Node playRoot;
  public Tab teamMatchmakingTab;
  public Tab customGamesTab;
  public Tab coopTab;
  public TabPane playRootTabPane;
  public TeamMatchmakingController teamMatchmakingController;
  public CustomGamesController customGamesController;
  public CoopController coopController;
  private boolean isHandlingEvent;
  private AbstractViewController<?> lastTabController;
  private Tab lastTab;

  @Override
  public void initialize() {
    lastTab = teamMatchmakingTab;
    lastTabController = teamMatchmakingController;
    playRootTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newValue == teamMatchmakingTab) {
        navigationHandler.navigateTo(new OpenTeamMatchmakingEvent());
      } else if (newValue == customGamesTab) {
        navigationHandler.navigateTo(new OpenCustomGamesEvent());
      } else if (newValue == coopTab) {
        navigationHandler.navigateTo(new OpenCoopEvent());
      }
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (navigateEvent instanceof OpenTeamMatchmakingEvent) {
        lastTab = teamMatchmakingTab;
        lastTabController = teamMatchmakingController;
      } else if (navigateEvent instanceof OpenCustomGamesEvent) {
        lastTab = customGamesTab;
        lastTabController = customGamesController;
      } else if (navigateEvent instanceof OpenCoopEvent) {
        lastTab = coopTab;
        lastTabController = coopController;
      }
      playRootTabPane.getSelectionModel().select(lastTab);
      lastTabController.display(navigateEvent);
    } finally {
      isHandlingEvent = false;
    }
  }

  @Override
  public void onHide() {
    teamMatchmakingController.onHide();
    customGamesController.onHide();
    coopController.onHide();
  }

  @Override
  public Node getRoot() {
    return playRoot;
  }
}
