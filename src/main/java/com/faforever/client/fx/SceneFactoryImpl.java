package com.faforever.client.fx;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;


public class SceneFactoryImpl implements SceneFactory {

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  FxmlLoader fxmlLoader;

  @Override
  public Scene createScene(Stage stage, Region root, boolean resizable, WindowDecorator.WindowButtonType... buttons) {
    String theme = preferencesService.getPreferences().getTheme();
    String themeCss = String.format("/themes/%s/style.css", theme);
    stage.setResizable(resizable);

    WindowDecorator windowDecorator = fxmlLoader.loadAndGetController("window.fxml");
    windowDecorator.configure(stage, root, resizable, buttons);

    Scene scene = new Scene(windowDecorator.getWindowRoot());
    scene.getStylesheets().add(themeCss);

    stage.setScene(scene);

    return scene;
  }
}
