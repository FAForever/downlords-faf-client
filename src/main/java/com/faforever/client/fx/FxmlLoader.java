package com.faforever.client.fx;


public interface FxmlLoader {

  /**
   * @param file the FXML file name, relative to its theme directory. E.g. "main.fxml" for "/themes/default/main.fxml"
   */
  <T> T loadAndGetController(String file);

  <T> T loadAndGetRoot(String file);

  <T> T loadAndGetRoot(String file, Object controller);
}
