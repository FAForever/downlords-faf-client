package com.faforever.client.game;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class GamePathHandler {
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  /**
   * Checks whether the chosen game path contains a ForgedAlliance.exe (either directly if the user selected the "bin"
   * directory, or in the "bin" sub folder). If the path is valid, it is stored in the preferences.
   */
  public CompletableFuture<Path> onGameDirectoryChosenEvent(Path path) {
    if (path == null) {
      notificationService.addImmediateWarnNotification("gamePath.select.noneChosen");
      return CompletableFuture.failedFuture(new CancellationException("User cancelled"));
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
      return CompletableFuture.failedFuture(e);
    }

    if (gamePathValidWithError != null) {
      notificationService.addImmediateWarnNotification(gamePathValidWithError);
      return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid path"));
    }

    log.info("Found game path at {}", gamePath);
    forgedAlliancePrefs.setInstallationPath(gamePath);
    return CompletableFuture.completedFuture(gamePath);
  }
}
