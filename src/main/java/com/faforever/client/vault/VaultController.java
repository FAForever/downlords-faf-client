package com.faforever.client.vault;

import com.faforever.client.map.MapService;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

public class VaultController {

  @Autowired
  MapService mapService;

  private Node root;

  public Node getRoot() {
    return new Pane();
  }

  public void setUpIfNecessary() {
    // FIXME test code so far
    mapService.getMapsFromVault(0, 10);
  }
}
