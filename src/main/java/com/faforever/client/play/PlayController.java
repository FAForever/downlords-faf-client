package com.faforever.client.play;

import com.faforever.client.coop.CoopController;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.CustomGamesController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.main.event.Open1v1Event;
import com.faforever.client.main.event.OpenCoopEvent;
import com.faforever.client.main.event.OpenCustomGamesEvent;
import com.faforever.client.rankedmatch.Ladder1v1Controller;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PlayController extends AbstractViewController<Node> {
  public Node playRoot;
  private final UiService uiService;
  private final EventBus eventBus;
  public CustomGamesController customGamesController;
  public Ladder1v1Controller ladderController;
  public CoopController coopController;
  public ToggleButton customGamesButton;
  public ToggleButton ladderButton;
  public ToggleButton coopButton;
  public ToggleGroup playNavigation;
  public AnchorPane contentPane;
  private boolean isHandlingEvent;


  public PlayController(UiService uiService, EventBus eventBus) {
    this.uiService = uiService;
    this.eventBus = eventBus;
  }

  @Override
  public void initialize() {
    playNavigation.selectedToggleProperty().addListener((observable, toggle, newToggle) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newToggle == customGamesButton) {
        eventBus.post(new OpenCustomGamesEvent());
      } else if (newToggle == ladderButton) {
        ladderButton.setSelected(true);
        eventBus.post(new Open1v1Event());
      } else if (newToggle == coopButton) {
        eventBus.post(new OpenCoopEvent());
      }
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (Objects.equals(navigateEvent.getClass(), NavigateEvent.class)
          || navigateEvent instanceof OpenCustomGamesEvent) {
        Node node = customGamesController.getRoot();
        contentPane.getChildren().add(node);
        JavaFxUtil.setAnchors(node, 0d);
        customGamesController.display(navigateEvent);
      }
      if (navigateEvent instanceof Open1v1Event) {
        Node node = ladderController.getRoot();
        contentPane.getChildren().add(node);
        JavaFxUtil.setAnchors(node, 0d);
        ladderController.display(navigateEvent);
      }
      if (navigateEvent instanceof OpenCoopEvent) {
        Node node = coopController.getRoot();
        contentPane.getChildren().add(node);
        JavaFxUtil.setAnchors(node, 0d);
        coopController.display(navigateEvent);
      }
    } finally {
      isHandlingEvent = false;
    }
  }

  @Override
  protected void onHide() {
    customGamesController.hide();
    ladderController.hide();
    coopController.hide();
  }

  @Override
  public Node getRoot() {
    return playRoot;
  }

  public void onCustomNavigateButtonClicked(ActionEvent actionEvent) {
  }

  public void on1v1NavigateButtonClicked(ActionEvent actionEvent) {
  }

  public void onCoopNavigateButtonClicked(ActionEvent actionEvent) {
  }

}