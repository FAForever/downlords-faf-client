package com.faforever.client.play;

import com.faforever.client.fx.NodeController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenCoopEvent;
import com.faforever.client.main.event.OpenCustomGamesEvent;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.theme.UiService;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlayController extends NodeController<Node> {

  private final NavigationHandler navigationHandler;
  private final UiService uiService;

  public Node playRoot;
  public Pane contentPane;
  public ToggleButton matchmakingButton;
  public ToggleButton customButton;
  public ToggleButton coopButton;

  private final Map<ToggleButton, PlayContentEnum> contentMap = new HashMap<>();

  @Override
  protected void onInitialize() {
    contentMap.putAll(Map.of(matchmakingButton, PlayContentEnum.MATCHMAKING, customButton, PlayContentEnum.CUSTOM,
                             coopButton, PlayContentEnum.COOP));
    PlayContentEnum lastTab = navigationHandler.getLastPlayTab();
    showContent(lastTab == null ? PlayContentEnum.MATCHMAKING : lastTab);
  }

  public void onNavigateButtonClicked(ActionEvent event) {
    if (event.getSource() instanceof ToggleButton toggleButton) {
      PlayContentEnum contentEnum = contentMap.getOrDefault(toggleButton, PlayContentEnum.MATCHMAKING);
      showContent(contentEnum);
    }
  }

  private NodeController<?> showContent(PlayContentEnum contentEnum) {
    contentMap.entrySet()
              .stream()
              .filter(entry -> contentEnum.equals(entry.getValue()))
              .map(Entry::getKey)
              .findFirst()
              .ifPresent(button -> button.setSelected(true));
    NodeController<?> controller = switch (contentEnum) {
      case MATCHMAKING -> uiService.loadFxml("theme/play/team_matchmaking.fxml");
      case CUSTOM -> uiService.loadFxml("theme/play/custom_games.fxml");
      case COOP -> uiService.loadFxml("theme/play/coop/coop.fxml");
    };
    contentPane.getChildren().setAll(controller.getRoot());
    navigationHandler.setLastPlayTab(contentEnum);
    return controller;
  }

  @Override
  protected void onNavigate(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof OpenTeamMatchmakingEvent) {
      showContent(PlayContentEnum.MATCHMAKING).display(navigateEvent);
    } else if (navigateEvent instanceof OpenCustomGamesEvent) {
      showContent(PlayContentEnum.CUSTOM).display(navigateEvent);
    } else if (navigateEvent instanceof OpenCoopEvent) {
      showContent(PlayContentEnum.COOP).display(navigateEvent);
    }
  }

  @Override
  public Node getRoot() {
    return playRoot;
  }

  public enum PlayContentEnum {
    MATCHMAKING, CUSTOM, COOP
  }
}
