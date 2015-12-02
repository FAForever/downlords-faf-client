package com.faforever.client.fx;

import com.faforever.client.ThemeService;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import javax.annotation.Resource;


public class StageConfiguratorImpl implements StageConfigurator {

  @Resource
  PreferencesService preferencesService;
  @Resource
  FxmlLoader fxmlLoader;
  @Resource
  ThemeService themeService;

  @Override
  public void configureScene(Stage stage, Region root, boolean resizable, WindowDecorator.WindowButtonType... buttons) {
    stage.setResizable(resizable);
    WindowDecorator windowDecorator = fxmlLoader.loadAndGetController("window.fxml");
    windowDecorator.configure(stage, root, resizable, buttons);

    Scene scene = new Scene(windowDecorator.getWindowRoot());
    stage.setScene(scene);
    scene.getStylesheets().add(themeService.getThemeFile("style.css"));
  }
}
