package com.faforever.client.play;

import com.faforever.client.coop.CoopController;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.game.CustomGamesController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.Open1v1Event;
import com.faforever.client.main.event.OpenCoopEvent;
import com.faforever.client.main.event.OpenCustomGamesEvent;
import com.faforever.client.rankedmatch.Ladder1v1Controller;
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
public class PlayController extends AbstractViewController<Node> {
  public Node playRoot;
  private final EventBus eventBus;
  public Tab customGamesTab;
  public Tab coopTab;
  public Tab ladderTab;
  public TabPane playRootTabPane;
  public CustomGamesController customGamesController;
  public Ladder1v1Controller ladderController;
  public CoopController coopController;
  private boolean isHandlingEvent;


  public PlayController(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void initialize() {
    eventBus.post(new OpenCustomGamesEvent());
    playRootTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newValue == customGamesTab) {
        eventBus.post(new OpenCustomGamesEvent());
      } else if (newValue == ladderTab) {
        eventBus.post(new Open1v1Event());
      } else if (newValue == coopTab) {
        eventBus.post(new OpenCoopEvent());
      }
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (navigateEvent instanceof OpenCustomGamesEvent) {
        playRootTabPane.getSelectionModel().select(customGamesTab);
        customGamesController.display(navigateEvent);
      }
      if (navigateEvent instanceof Open1v1Event) {
        playRootTabPane.getSelectionModel().select(ladderTab);
        ladderController.display(navigateEvent);
      }
      if (navigateEvent instanceof OpenCoopEvent) {
        playRootTabPane.getSelectionModel().select(coopTab);
        coopController.display(navigateEvent);
      }
    } finally {
      isHandlingEvent = false;
    }
  }

  @Override
  public Node getRoot() {
    return playRoot;
  }

}
