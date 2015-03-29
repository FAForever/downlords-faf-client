package com.faforever.client.fxml;


public interface FxmlLoader {

  void setTheme(String theme);

  /**
   * @param file the FXML file name, relative to its theme directory. E.g. "main.fxml" for "/themes/default/main.fxml"
   */
  <T> T loadAndGetController(String file);

  /**
   * Loads the given FXML file and sets the given {@code root} as root and controller. This is used for custom controls
   * that use "&lt;fx:root&gt;"
   *
   * @param file the FXML file name, relative to its theme directory. E.g. "main.fxml" for "/themes/default/main.fxml"
   * @param control
   */
  void loadCustomControl(String file, Object control);
}
