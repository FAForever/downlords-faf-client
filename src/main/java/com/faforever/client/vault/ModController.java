package com.faforever.client.vault;

import com.faforever.client.fx.NodeController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModController extends NodeController<Node> {
  private final UiService uiService;
  public StackPane root;
  public ModVaultController modVaultController;

  public ModController(UiService uiService) {
    this.uiService = uiService;
  }

  @Override
  public Node getRoot() {
    return root;
  }

  @Override
  protected void onInitialize() {
    modVaultController = uiService.loadFxml("theme/vault/vault_entity.fxml", ModVaultController.class);
    root.getChildren().add(modVaultController.getRoot());
  }

  @Override
  protected void onNavigate(NavigateEvent navigateEvent) {
    modVaultController.display(navigateEvent);
  }
}
