package com.faforever.client.vault;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenMapVaultEvent;
import com.faforever.client.main.event.OpenModVaultEvent;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.replay.OnlineReplayVaultController;
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
  public Tab onlineReplayVaultTab;
  private boolean isHandlingEvent;

  public VaultController(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Node getRoot() {
    return vaultRoot;
  }

  @Override
  public void initialize() {
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
      }
      // TODO implement other tabs
    });
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    isHandlingEvent = true;

    try {
      if (Objects.equals(navigateEvent.getClass(), NavigateEvent.class)
          || navigateEvent instanceof OpenMapVaultEvent) {
        vaultRoot.getSelectionModel().select(mapVaultTab);
        mapVaultController.onDisplay(navigateEvent);
      }
      if (navigateEvent instanceof OpenModVaultEvent) {
        vaultRoot.getSelectionModel().select(modVaultTab);
        modVaultController.onDisplay(navigateEvent);
      }
      if (navigateEvent instanceof OpenOnlineReplayVaultEvent) {
        vaultRoot.getSelectionModel().select(onlineReplayVaultTab);
        onlineReplayVaultController.onDisplay(navigateEvent);
      }
    } finally {
      isHandlingEvent = false;
    }
  }
}
