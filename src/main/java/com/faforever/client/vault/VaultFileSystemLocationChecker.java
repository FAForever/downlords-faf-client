package com.faforever.client.vault;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.base.CharMatcher;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Pattern;

import static org.bridj.Platform.isUnix;
import static org.bridj.Platform.isWindows;

/**
 * Checks if the user is on linux, has a none ascii username or has One Drive installed. In this cases we need to use a fallback location for the
 * vault.
 */
@Component
public class VaultFileSystemLocationChecker {
  public static final Pattern ONE_DRIVE_FOLDER_NAME_IDENTIFIER = Pattern.compile("[Oo]ne[ ]*[Dd]rive");
  private final NotificationService notificationService;
  private final PreferencesService preferencesService;
  private final ForgedAlliancePrefs forgedAlliance;
  private final I18n i18n;

  public VaultFileSystemLocationChecker(NotificationService notificationService, PreferencesService preferencesService, I18n i18n) {
    this.notificationService = notificationService;
    this.preferencesService = preferencesService;
    this.forgedAlliance = preferencesService.getPreferences().getForgedAlliance();

    this.i18n = i18n;
  }

  public void checkVaultFileSystemLocation() {
    ForgedAlliancePrefs forgedAlliance = preferencesService.getPreferences().getForgedAlliance();
    if (forgedAlliance.isVaultCheckDone()) {
      return;
    }
    String userName = System.getProperty("user.name");

    if (userName != null && !CharMatcher.ascii().matchesAllOf(userName) && isWindows()) {
      setSecondaryVaultLocationAndNotifyUser("mapDir.changed.noneAscii");
    } else {
      String myDocumentsParentFolder = preferencesService.getPrimaryVaultLocation().getParent().toString();
      if (isWindows() && ONE_DRIVE_FOLDER_NAME_IDENTIFIER.matcher(myDocumentsParentFolder).matches()) {
        setSecondaryVaultLocationAndNotifyUser("mapDir.changed.oneDrive");
      }
    }

    if (isUnix()) {
      setSecondaryVaultLocationAndNotifyUser("mapDir.changed.unix");
    }

    forgedAlliance.setVaultCheckDone(true);
    preferencesService.storeInBackground();
  }

  private void setSecondaryVaultLocationAndNotifyUser(String messageKey) {
    Action revertAction = new Action(i18n.get("revert.alreadySymlinked"), event -> forgedAlliance.setVaultBaseDirectory(preferencesService.getPrimaryVaultLocation()));
    forgedAlliance.setVaultBaseDirectory(preferencesService.getSecondaryVaultLocation());
    PersistentNotification notification = new PersistentNotification(i18n.get(messageKey), Severity.INFO, Collections.singletonList(revertAction));
    notificationService.addNotification(notification);
  }
}
