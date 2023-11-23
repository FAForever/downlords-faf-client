package com.faforever.client.vault;

import com.faforever.client.fx.NodeController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenLiveReplayViewEvent;
import com.faforever.client.main.event.OpenLocalReplayVaultEvent;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.replay.LocalReplayVaultController;
import com.faforever.client.replay.OnlineReplayVaultController;
import com.faforever.client.theme.UiService;
import javafx.event.ActionEvent;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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
public class ReplayController extends NodeController<VBox> {

  private final NavigationHandler navigationHandler;
  private final UiService uiService;

  public VBox root;
  public Pane contentPane;
  public ToggleButton onlineButton;
  public ToggleButton liveButton;
  public ToggleButton localButton;

  private final Map<ToggleButton, ReplayContentEnum> contentMap = new HashMap<>();

  @Override
  public VBox getRoot() {
    return root;
  }

  @Override
  protected void onInitialize() {
    contentMap.putAll(Map.of(onlineButton, ReplayContentEnum.ONLINE, liveButton, ReplayContentEnum.LIVE,
                             localButton, ReplayContentEnum.LOCAL));
    ReplayContentEnum lastTab = navigationHandler.getLastReplayTab();
    showContent(lastTab == null ? ReplayContentEnum.ONLINE : lastTab);
  }

  public void onNavigateButtonClicked(ActionEvent event) {
    if (event.getSource() instanceof ToggleButton toggleButton) {
      ReplayContentEnum contentEnum = contentMap.getOrDefault(toggleButton, ReplayContentEnum.ONLINE);
      showContent(contentEnum);
    }
  }

  private NodeController<?> showContent(ReplayContentEnum contentEnum) {
    contentMap.entrySet()
              .stream()
              .filter(entry -> contentEnum.equals(entry.getValue()))
              .map(Entry::getKey)
              .findFirst()
              .ifPresent(button -> button.setSelected(true));
    NodeController<?> controller = switch (contentEnum) {
      case ONLINE -> uiService.loadFxml("theme/vault/vault_entity.fxml", OnlineReplayVaultController.class);
      case LIVE -> uiService.loadFxml("theme/vault/replay/live_replays.fxml");
      case LOCAL -> uiService.loadFxml("theme/vault/vault_entity.fxml", LocalReplayVaultController.class);
    };
    contentPane.getChildren().setAll(controller.getRoot());
    navigationHandler.setLastReplayTab(contentEnum);
    return controller;
  }

  @Override
  protected void onNavigate(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof OpenOnlineReplayVaultEvent) {
      showContent(ReplayContentEnum.ONLINE).display(navigateEvent);
    } else if (navigateEvent instanceof OpenLiveReplayViewEvent) {
      showContent(ReplayContentEnum.LIVE).display(navigateEvent);
    } else if (navigateEvent instanceof OpenLocalReplayVaultEvent) {
      showContent(ReplayContentEnum.LOCAL).display(navigateEvent);
    }
  }

  public enum ReplayContentEnum {
    ONLINE, LIVE, LOCAL;
  }
}
