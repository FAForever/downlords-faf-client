package com.faforever.client.vault;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapController extends AbstractViewController<Node> {
  private final UiService uiService;
  public StackPane root;
  public MapVaultController mapVaultController;

  public MapController(UiService uiService) {
    this.uiService = uiService;
  }

  @Override
  public Node getRoot() {
    return root;
  }

  @Override
  public void initialize() {
    mapVaultController = uiService.loadFxml("theme/vault/vault_entity.fxml", MapVaultController.class);
    root.getChildren().add(mapVaultController.getRoot());
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    mapVaultController.display(navigateEvent);
  }
}
