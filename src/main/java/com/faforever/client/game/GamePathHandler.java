package com.faforever.client.game;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
  public void onGameDirectoryChosenEvent(GameDirectoryChosenEvent event) {
    Path path = event.path();
    Optional<CompletableFuture<Path>> future = Optional.ofNullable(event.future());

    if (path == null) {
      notificationService.addImmediateWarnNotification("gamePath.select.noneChosen");
      future.ifPresent(pathCompletableFuture -> pathCompletableFuture.completeExceptionally(new CancellationException("User cancelled")));
      return;
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
      future.ifPresent(pathCompletableFuture -> pathCompletableFuture.completeExceptionally(e));
      return;
    }

    if (gamePathValidWithError != null) {
      notificationService.addImmediateWarnNotification(gamePathValidWithError);
      future.ifPresent(pathCompletableFuture -> pathCompletableFuture.completeExceptionally(new IllegalArgumentException("Invalid path")));
      return;
    }

    log.info("Found game path at {}", gamePath);
    forgedAlliancePrefs.setInstallationPath(gamePath);
    future.ifPresent(pathCompletableFuture -> pathCompletableFuture.complete(gamePath));
  }
}
