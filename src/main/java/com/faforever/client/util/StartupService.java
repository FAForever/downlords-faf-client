package com.faforever.client.util;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.ForgedAlliancePrefs;
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
public class StartupService {

  private final OperatingSystem operatingSystem;
  private final I18n i18n;
  private final GeneralPrefs generalPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private void checkCyrillic() {
    if (!generalPrefs.isShowCyrillicWarning()) {
      return;
    }

    String preferencesDirectoryString = String.valueOf(operatingSystem.getPreferencesDirectory());
    log.debug("Current preferences directory " + preferencesDirectoryString);

    if (!preferencesDirectoryString.matches(".*[а-яА-ЯёЁ].*")) {
      return;
    }

    fxApplicationThreadExecutor.executeAndWait(() -> {
      Alert alert = new Alert(AlertType.WARNING, i18n.get("home.directory.warning.cyrillic"),
          new ButtonType(i18n.get("ignoreWarning"), ButtonData.CANCEL_CLOSE),
          new ButtonType(i18n.get("close"), ButtonData.OK_DONE));
      alert.showAndWait()
          .filter(button -> button.getButtonData() == ButtonData.CANCEL_CLOSE)
          .ifPresent(button -> generalPrefs.setShowCyrillicWarning(false));
    });
  }

  private void checkAdmin() {
    if (!operatingSystem.runsAsAdmin()) {
      return;
    }

    fxApplicationThreadExecutor.executeAndWait(() -> {
      Alert alert = new Alert(AlertType.WARNING, "Please don't run the client as admin. Because if you do you might need to delete C:\\ProgramData\\FAForever to be able to run it as a normal user again. Do you want to ignore the warning and continue?", ButtonType.YES, ButtonType.NO);
      alert.showAndWait()
          .filter(buttonType -> buttonType == ButtonType.NO)
          .ifPresent(buttonType -> {
            log.info("The user refused to run the app with admin rights. Closing the app");
            System.exit(0);
          });
    });
  }

  private void checkOneDrive() {
    if (!forgedAlliancePrefs.getVaultBaseDirectory().toString().contains("OneDrive")) {
      return;
    }

    fxApplicationThreadExecutor.executeAndWait(() -> {
      Alert alert = new Alert(AlertType.WARNING, "Maps and Mods path detected to be in One Drive. One Drive can cause issues during the game as it has a lock on the files  during synchronization. It is advised that you change your maps and mods path in the client settings", ButtonType.OK);
      alert.showAndWait();
    });
  }

  @EventListener(value = ApplicationReadyEvent.class)
  public void checkSystem() {
    checkAdmin();
    checkCyrillic();
    checkOneDrive();
  }
}
