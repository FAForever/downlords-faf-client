package com.faforever.client.fx;

import com.faforever.client.preferences.PreferencesService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;

import javax.annotation.Resource;

public class DialogFactoryImpl implements DialogFactory {

  @Resource
  PreferencesService preferencesService;

  @Resource
  FxmlLoader fxmlLoader;

  @Override
  public Alert createAlert(Alert.AlertType alertType, String text, ButtonType... buttons) {
    String theme = preferencesService.getPreferences().getThemeName();
    String themeCss = String.format("/themes/%s/style.css", theme);

    Alert alert = new Alert(alertType);
    alert.initStyle(StageStyle.TRANSPARENT);
    alert.setDialogPane(fxmlLoader.loadAndGetRoot("dialog.fxml"));
    alert.getDialogPane().getScene().getStylesheets().add(themeCss);
    alert.getButtonTypes().setAll(buttons);
    alert.setContentText(text);

    return alert;
  }
}
