package com.faforever.client.fxml;

import javafx.scene.Node;
import org.springframework.core.io.Resource;


public interface FxmlLoader {

  <T> T loadAndGetController(Resource resource);

  <T> T loadAndGetController(String file);

  <T extends Node> T loadAndGetRoot(String file);

  <T extends Node> T loadAndGetRoot(Resource resource);
}
