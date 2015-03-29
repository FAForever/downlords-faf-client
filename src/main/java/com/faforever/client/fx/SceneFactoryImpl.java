package com.faforever.client.fx;

import com.faforever.client.preferences.PreferencesService;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.springframework.beans.factory.annotation.Autowired;

public class SceneFactoryImpl implements SceneFactory {

  @Autowired
  PreferencesService preferencesService;

  @Override
  public Scene createScene(Parent root) {
    String theme = preferencesService.getPreferences().getTheme();
    String path = String.format("/themes/%s/style.css", theme);

    Scene scene = new Scene(root);
    scene.getStylesheets().add(path);
    return scene;
  }
}
