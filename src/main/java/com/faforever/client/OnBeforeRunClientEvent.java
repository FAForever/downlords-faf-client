package com.faforever.client;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.GeneralPrefs;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnBeforeRunClientEvent {

  private final OperatingSystem operatingSystem;
  private final I18n i18n;
  private final GeneralPrefs generalPrefs;

  private void checkCyrillic() {
    String preferencesDirectoryString = String.valueOf(operatingSystem.getPreferencesDirectory());
    log.debug("Current preferences directory " + preferencesDirectoryString);

    if (preferencesDirectoryString.matches(".*[а-яА-ЯёЁ].*")) {
      log.warn("Path of preferences directory contains cyrillic characters!");
      if (generalPrefs.getShowCyrillicWarning()) {
        ButtonType ignoreWarningButton = new ButtonType(i18n.get("ignoreWarning"), ButtonData.LEFT);
        ButtonType closeButton = new ButtonType(i18n.get("close"), ButtonData.CANCEL_CLOSE);
        JavaFxUtil.runLaterAndAwait(() -> {
          Alert alert = new Alert(AlertType.WARNING, i18n.get("home.directory.warning.cyrillic"), ignoreWarningButton, closeButton);
          alert.showAndWait()
              .filter(button -> button.getButtonData() == ButtonData.LEFT)
              .ifPresent(button -> generalPrefs.setShowCyrillicWarning(false));
        });
      }
    }
  }

  private void checkAdmin() {
    if (operatingSystem.runsAsAdmin()) {
      JavaFxUtil.runLaterAndAwait(() -> {
        Alert alert = new Alert(AlertType.WARNING, "Please don't run the client as admin. Because if you do you might need to delete C:\\ProgramData\\FAForever to be able to run it as a normal user again. Do you want to ignore the warning and continue?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait()
            .filter(buttonType -> buttonType == ButtonType.NO)
            .ifPresent(buttonType -> {
              log.info("The user refused to run the app with admin rights. Closing the app");
              System.exit(0);
            });
      });
    }
  }

  @EventListener(value = ApplicationReadyEvent.class)
  public void handle() {
    checkAdmin();
    checkCyrillic();
  }
}
