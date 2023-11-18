package com.faforever.client.vault;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLiveReplayViewEvent;
import com.faforever.client.main.event.OpenLocalReplayVaultEvent;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.replay.LiveReplayController;
import com.faforever.client.replay.LocalReplayVaultController;
import com.faforever.client.replay.OnlineReplayVaultController;
import com.faforever.client.theme.UiService;
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
public class ReplayController extends AbstractViewController<Node> {

  private final NavigationHandler navigationHandler;
  private final UiService uiService;

  public TabPane root;

  public OnlineReplayVaultController onlineReplayVaultController;
  public LocalReplayVaultController localReplayVaultController;
  public LiveReplayController liveReplayController;
  public Tab onlineReplayVaultTab;
  public Tab localReplayVaultTab;
  public Tab liveReplayVaultTab;
  private boolean isHandlingEvent;
  private AbstractViewController<?> lastTabController;
  private Tab lastTab;

  @Override
  public TabPane getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    onlineReplayVaultController = uiService.loadFxml("theme/vault/vault_entity.fxml", OnlineReplayVaultController.class);
    onlineReplayVaultTab.setContent(onlineReplayVaultController.getRoot());
    localReplayVaultController = uiService.loadFxml("theme/vault/vault_entity.fxml", LocalReplayVaultController.class);
    localReplayVaultTab.setContent(localReplayVaultController.getRoot());
    liveReplayController = uiService.loadFxml("theme/vault/replay/live_replays.fxml");
    liveReplayVaultTab.setContent(liveReplayController.getRoot());
    lastTab = onlineReplayVaultTab;
    lastTabController = onlineReplayVaultController;
    root.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newValue == onlineReplayVaultTab) {
        navigationHandler.navigateTo(new OpenOnlineReplayVaultEvent());
      } else if (newValue == localReplayVaultTab) {
        navigationHandler.navigateTo(new OpenLocalReplayVaultEvent());
      } else if (newValue == liveReplayVaultTab) {
        navigationHandler.navigateTo(new OpenLiveReplayViewEvent());
      }
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (navigateEvent instanceof OpenOnlineReplayVaultEvent) {
        lastTab = onlineReplayVaultTab;
        lastTabController = onlineReplayVaultController;
      } else if (navigateEvent instanceof OpenLocalReplayVaultEvent) {
        lastTab = localReplayVaultTab;
        lastTabController = localReplayVaultController;
      } else if (navigateEvent instanceof OpenLiveReplayViewEvent) {
        lastTab = liveReplayVaultTab;
        lastTabController = liveReplayController;
      }
      root.getSelectionModel().select(lastTab);
      lastTabController.display(navigateEvent);
    } finally {
      isHandlingEvent = false;
    }
  }
}
