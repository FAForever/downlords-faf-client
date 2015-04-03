package com.faforever.client.fx;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;

public class SceneFactoryImpl implements SceneFactory {

  public enum WindowButtonType {
    MINIMIZE,
    MAXIMIZE_RESTORE,
    CLOSE;
  }

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  FxmlLoader fxmlLoader;

  @Override
  public Scene createScene(Stage stage, Parent root, boolean resizable, WindowButtonType... buttons) {
    String theme = preferencesService.getPreferences().getTheme();
    String themeCss = String.format("/themes/%s/style.css", theme);
    stage.setResizable(resizable);

    WindowDecorator windowDecorator = fxmlLoader.loadAndGetController("window.fxml");
    windowDecorator.configure(stage, root, resizable, buttons);

    Scene scene = new Scene(windowDecorator.getWindowRoot());
    scene.getStylesheets().add(themeCss);

    return scene;
  }
}
