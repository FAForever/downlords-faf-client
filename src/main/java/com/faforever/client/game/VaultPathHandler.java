package com.faforever.client.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.CancelAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.OkAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.tasks.MoveDirectoryTask;
import com.faforever.client.task.TaskService;
import javafx.scene.control.CheckBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VaultPathHandler {

  private final PlatformService platformService;
  private final TaskService taskService;
  private final ApplicationContext applicationContext;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  public void verifyVaultPathAndShowWarning() {
    if (preferencesService.isVaultBasePathInvalidForAscii()) {
      log.warn("Vault base path contains non ASCII characters: {}", forgedAlliancePrefs.getVaultBaseDirectory());
      if (forgedAlliancePrefs.getWarnNonAsciiVaultPath()) {
        showWarning();
      }
    }
  }

  private void showWarning() {
    CheckBox ignoreCheckbox = new CheckBox(i18n.get("vaultBasePath.nonAscii.warning.ignore"));
    JavaFxUtil.addListener(ignoreCheckbox.selectedProperty(), (observable, oldValue, newValue) -> {
      forgedAlliancePrefs.setWarnNonAsciiVaultPath(!newValue);
    });

    notificationService.addImmediateWarnNotification(
        i18n.get("vaultBasePath.nonAscii.warning.title"),
        i18n.get("vaultBasePath.nonAscii.warning.text"),
        List.of(new Action(i18n.get("vaultBasePath.nonAscii.warning.changePath"), event -> platformService.askForPath(i18n.get("settings.vault.select")).ifPresent(this::onVaultPathUpdated)),
            new OkAction(i18n)),
        ignoreCheckbox);
  }

  public void onVaultPathUpdated(Path newPath) {
    log.info("User changed vault directory to: `{}`", newPath);

    MoveDirectoryTask moveDirectoryTask = applicationContext.getBean(MoveDirectoryTask.class);
    moveDirectoryTask.setOldDirectory(forgedAlliancePrefs.getVaultBaseDirectory());
    moveDirectoryTask.setNewDirectory(newPath);
    moveDirectoryTask.setAfterCopyAction(() -> forgedAlliancePrefs.setVaultBaseDirectory(newPath));
    notificationService.addNotification(new ImmediateNotification(i18n.get("settings.vault.change"), i18n.get("settings.vault.change.message"), Severity.INFO,
        List.of(
            new Action(i18n.get("no"), event -> {
              moveDirectoryTask.setPreserveOldDirectory(false);
              taskService.submitTask(moveDirectoryTask);
            }),
            new Action(i18n.get("yes"), event -> {
              moveDirectoryTask.setPreserveOldDirectory(true);
              taskService.submitTask(moveDirectoryTask);
            }),
            new CancelAction(i18n)
        )));
  }
}
