package com.faforever.client.fxml;

import org.springframework.core.io.Resource;

public interface FxmlLoader {

  public <T> T load(Resource resource);

  public <T> T load(String file);
}
