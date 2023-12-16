package com.faforever.client.game;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class GamePathHandler implements InitializingBean {
  private static final Collection<Path> USUAL_GAME_PATHS = Arrays.asList(
      Path.of(System.getenv("ProgramFiles") + "\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + " (x86)\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + " (x86)\\Steam\\steamapps\\common\\supreme commander forged alliance"),
      Path.of(System.getProperty("user.home"), ".steam", "steam", "steamapps", "common",
              "Supreme Commander Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + "\\Supreme Commander - Forged Alliance")
  );

  private final LoginService loginService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final PlatformService platformService;
  private final I18n i18n;

  @Override
  public void afterPropertiesSet() {
    loginService.loggedInProperty().subscribe(loggedIn -> {
      if (loggedIn) {
        detectAndUpdateGamePath();
      }
    });
  }

  public void notifyMissingGamePath(boolean immediateUserActionRequired) {
    List<Action> actions = Collections.singletonList(
        new Action(i18n.get("missingGamePath.locate"),
                   chooseEvent -> chooseAndValidateGameDirectory())
    );
    String notificationText = i18n.get("missingGamePath.notification");

    if (immediateUserActionRequired) {
      notificationService.addNotification(
          new ImmediateNotification(notificationText, notificationText, Severity.WARN, actions));
    } else {
      notificationService.addNotification(new PersistentNotification(notificationText, Severity.WARN, actions));
    }
  }

  private void detectGamePath() {
    for (Path path : USUAL_GAME_PATHS) {
      if (preferencesService.isValidGamePath(path.resolve("bin"))) {
        validateGamePath(path);
        return;
      }
    }

    log.warn("Game path could not be detected");
    notifyMissingGamePath(false);
  }

  private void detectAndUpdateGamePath() {
    Path faPath = forgedAlliancePrefs.getInstallationPath();
    if (faPath == null || Files.notExists(faPath)) {
      log.info("Game path is not specified or non-existent, trying to detect");
      detectGamePath();
    }
  }

  public CompletableFuture<Void> chooseAndValidateGameDirectory() {
    return platformService.askForPath(i18n.get("missingGamePath.chooserTitle"))
                          .thenAccept(possiblePath -> validateGamePath(possiblePath.orElse(null)));
  }

  /**
   * Checks whether the chosen game path contains a ForgedAlliance.exe (either directly if the user selected the "bin"
   * directory, or in the "bin" sub folder). If the path is valid, it is stored in the preferences.
   */
  private void validateGamePath(Path path) {
    if (path == null) {
      notificationService.addImmediateWarnNotification("gamePath.select.noneChosen");
      throw new IllegalArgumentException("No game path chosen");
    }

    Path pathWithBin = path.resolve("bin");
    if (Files.isDirectory(pathWithBin)) {
      path = pathWithBin;
    }
    // At this point, path points to the "bin" directory
    Path gamePath = path.getParent();

    String gamePathValidWithError;
    try {
      gamePathValidWithError = preferencesService.isGamePathValidWithErrorMessage(gamePath);
    } catch (Exception e) {
      log.error("Game path selection error", e);
      notificationService.addImmediateErrorNotification(e, "gamePath.select.error");
      throw new IllegalStateException("Error while validating game path", e);
    }

    if (gamePathValidWithError != null) {
      notificationService.addImmediateWarnNotification(gamePathValidWithError);
      throw new IllegalArgumentException("Invalid path");
    }

    log.info("Found game path at {}", gamePath);
    forgedAlliancePrefs.setInstallationPath(gamePath);
  }
}
