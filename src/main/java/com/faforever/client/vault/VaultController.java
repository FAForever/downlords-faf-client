package com.faforever.client.vault;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenMapVaultEvent;
import com.faforever.client.main.event.OpenModVaultEvent;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.main.event.OpenReplayVaultEvent;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.replay.OnlineReplayVaultController;
import com.faforever.client.vault.replay.ReplayVaultController;
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
public class VaultController extends AbstractViewController<Node> {
  public TabPane vaultRoot;
  // TODO change to spring event bus
  private final EventBus eventBus;
  public Tab mapVaultTab;
  public Tab modVaultTab;
  public MapVaultController mapVaultController;
  public ModVaultController modVaultController;
  public OnlineReplayVaultController onlineReplayVaultController;
  public ReplayVaultController localReplayVaultController;
  public Tab onlineReplayVaultTab;
  public Tab localReplayVaultTab;
  private boolean isHandlingEvent;
  private AbstractViewController<?> lastTabController;
  private Tab lastTab;

  public VaultController(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Node getRoot() {
    return vaultRoot;
  }

  @Override
  public void initialize() {
    lastTab = onlineReplayVaultTab;
    lastTabController = onlineReplayVaultController;
    vaultRoot.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (isHandlingEvent) {
        return;
      }

      if (newValue == mapVaultTab) {
        eventBus.post(new OpenMapVaultEvent());
      } else if (newValue == modVaultTab) {
        eventBus.post(new OpenModVaultEvent());
      } else if (newValue == onlineReplayVaultTab) {
        eventBus.post(new OpenOnlineReplayVaultEvent());
      } else if (newValue == localReplayVaultTab) {
        eventBus.post(new OpenReplayVaultEvent());
      }
      // TODO implement other tabs
    });
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (navigateEvent instanceof OpenMapVaultEvent) {
        lastTab = mapVaultTab;
        lastTabController = mapVaultController;
      } else if (navigateEvent instanceof OpenModVaultEvent) {
        lastTab = modVaultTab;
            lastTabController = modVaultController;
      } else if (navigateEvent instanceof OpenOnlineReplayVaultEvent) {
        lastTab = onlineReplayVaultTab;
        lastTabController = onlineReplayVaultController;
      } else if (navigateEvent instanceof OpenReplayVaultEvent) {
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
