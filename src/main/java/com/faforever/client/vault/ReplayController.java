package com.faforever.client.vault;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLocalReplayVaultEvent;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.replay.LocalReplayVaultController;
import com.faforever.client.replay.OnlineReplayVaultController;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReplayController extends AbstractViewController<Node> {
  // TODO change to spring event bus
  private final EventBus eventBus;
  private final UiService uiService;
  public TabPane vaultRoot;

  public OnlineReplayVaultController onlineReplayVaultController;
  public LocalReplayVaultController localReplayVaultController;
  public Tab onlineReplayVaultTab;
  public Tab localReplayVaultTab;
  private boolean isHandlingEvent;
  private AbstractViewController<?> lastTabController;
  private Tab lastTab;

  public ReplayController(EventBus eventBus, UiService uiService) {
    this.eventBus = eventBus;
    this.uiService = uiService;
  }

  @Override
  public Node getRoot() {
    return vaultRoot;
  }

  @Override
  public void initialize() {
    onlineReplayVaultController = uiService.loadFxml("theme/vault/vault_entity.fxml", OnlineReplayVaultController.class);
    onlineReplayVaultTab.setContent(onlineReplayVaultController.getRoot());
    localReplayVaultController = uiService.loadFxml("theme/vault/vault_entity.fxml", LocalReplayVaultController.class);
    localReplayVaultTab.setContent(localReplayVaultController.getRoot());
    lastTab = onlineReplayVaultTab;
    lastTabController = onlineReplayVaultController;
    vaultRoot.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newValue == onlineReplayVaultTab) {
        eventBus.post(new OpenOnlineReplayVaultEvent());
      } else if (newValue == localReplayVaultTab) {
        eventBus.post(new OpenLocalReplayVaultEvent());
      }
      // TODO implement other tabs
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
      }
      vaultRoot.getSelectionModel().select(lastTab);
      lastTabController.display(navigateEvent);
    } finally {
      isHandlingEvent = false;
    }
  }
}
