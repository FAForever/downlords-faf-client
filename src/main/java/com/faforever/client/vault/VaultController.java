package com.faforever.client.vault;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.map.MapVaultController;
import com.faforever.client.mod.ModVaultController;
import com.faforever.client.vault.replay.ReplayVaultController;
import javafx.scene.Node;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class VaultController extends AbstractViewController<Node> {
  public Node vaultRoot;
  public MapVaultController mapVaultController;
  public ModVaultController modVaultController;
  public ReplayVaultController localReplayVaultController;
  public ReplayVaultController onlineReplayVaultController;

  @Override
  public Node getRoot() {
    return vaultRoot;
  }
}
