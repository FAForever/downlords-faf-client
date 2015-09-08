package com.faforever.client.fx;


public interface FxmlLoader {

  /**
   * @param file the FXML file name, relative to its theme directory. E.g. "main.fxml" for "/themes/default/main.fxml"
   */
  <T> T loadAndGetController(String file);

  /**
   * Loads the given FXML file and sets the given {@code root} as root and controller. This is used for custom controls
   * that use "&lt;fx:root&gt;"
   *
   * @param file the FXML file name, relative to its theme directory. E.g. "main.fxml" for "/themes/default/main.fxml"
   */
  void loadCustomControl(String file, Object control);

  <T> T loadAndGetRoot(String file);

  <T> T loadAndGetRoot(String file, Object controller);
}
