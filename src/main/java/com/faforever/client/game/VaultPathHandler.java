package com.faforever.client.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.SimpleChangeListener;
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
import com.faforever.client.user.LoginService;
import javafx.scene.control.CheckBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VaultPathHandler implements InitializingBean {

  private final PlatformService platformService;
  private final TaskService taskService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final LoginService loginService;
  private final I18n i18n;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final ObjectFactory<MoveDirectoryTask> moveDirectoryTaskFactory;

  @Override
  public void afterPropertiesSet() throws Exception {
    loginService.loggedInProperty().addListener((SimpleChangeListener<Boolean>) loggedIn -> {
      if (loggedIn) {
        verifyVaultPathAndShowWarning();
      }
    });
  }

  public void verifyVaultPathAndShowWarning() {
    if (preferencesService.isVaultBasePathInvalidForAscii()) {
      log.warn("Vault base path contains non ASCII characters: {}", forgedAlliancePrefs.getVaultBaseDirectory());
      if (forgedAlliancePrefs.getWarnNonAsciiVaultPath()) {
        showWarning();
      }
    }
  }

  private void showWarning() {
    CheckBox ignoreCheckbox = new CheckBox(i18n.get("ignoreWarning"));
    JavaFxUtil.addListener(ignoreCheckbox.selectedProperty(), (SimpleChangeListener<Boolean>) newValue -> forgedAlliancePrefs.setWarnNonAsciiVaultPath(!newValue));

    notificationService.addImmediateWarnNotification(i18n.get("vaultBasePath.nonAscii.warning.title"),
                                                     i18n.get("vaultBasePath.nonAscii.warning.text"), List.of(
            new Action(i18n.get("vaultBasePath.nonAscii.warning.changePath"), event -> askForPathAndUpdate()),
            new OkAction(i18n)), ignoreCheckbox);
  }

  public void askForPathAndUpdate() {
    platformService.askForPath(i18n.get("settings.vault.select"))
                   .thenAccept(possiblePath -> possiblePath.ifPresent(this::onVaultPathUpdated));
  }

  private void onVaultPathUpdated(Path newPath) {
    log.info("User changed vault directory to: `{}`", newPath);

    MoveDirectoryTask moveDirectoryTask = moveDirectoryTaskFactory.getObject();
    moveDirectoryTask.setOldDirectory(forgedAlliancePrefs.getVaultBaseDirectory());
    moveDirectoryTask.setNewDirectory(newPath);
    moveDirectoryTask.setAfterCopyAction(() -> forgedAlliancePrefs.setVaultBaseDirectory(newPath));
    notificationService.addNotification(new ImmediateNotification(i18n.get("settings.vault.change"), i18n.get("settings.vault.change.message"), Severity.INFO, List.of(new Action(i18n.get("no"), event -> {
      moveDirectoryTask.setPreserveOldDirectory(false);
      taskService.submitTask(moveDirectoryTask);
    }), new Action(i18n.get("yes"), event -> {
      moveDirectoryTask.setPreserveOldDirectory(true);
      taskService.submitTask(moveDirectoryTask);
    }), new CancelAction(i18n))));
  }
}
