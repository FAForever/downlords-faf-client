package com.faforever.client.notification;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.control.CheckBox;

import java.util.List;

import static com.faforever.client.notification.Severity.WARN;

public class WarnVaultBasePathNotification extends ImmediateNotification {

  public WarnVaultBasePathNotification(I18n i18n, PreferencesService preferencesService, ActionCallback doItAnywayActionCallback) {
    super(i18n.get("vaultBasePath.warning.title"),
        i18n.get("vaultBasePath.warning.description"),
        WARN,
        List.of(new CancelAction(i18n), new Action(i18n.get("vaultBasePath.warning.doItAnyway"), doItAnywayActionCallback)),
        () -> {
          CheckBox checkBox = new CheckBox(i18n.get("vaultBasePath.warning.dontShowWarning"));
          JavaFxUtil.bindBidirectional(checkBox.selectedProperty(), preferencesService.getPreferences().getForgedAlliance().warnNonAsciiVaultPathProperty());
          return checkBox;
        });
  }
}
