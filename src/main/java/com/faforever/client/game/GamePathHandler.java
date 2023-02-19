package com.faforever.client.game;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class GamePathHandler implements InitializingBean {
  private static final Collection<Path> USUAL_GAME_PATHS = Arrays.asList(
      Path.of(System.getenv("ProgramFiles") + "\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + " (x86)\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + " (x86)\\Steam\\steamapps\\common\\supreme commander forged alliance"),
      Path.of(System.getProperty("user.home"), ".steam", "steam", "steamapps", "common", "Supreme Commander Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + "\\Supreme Commander - Forged Alliance")
  );

  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final EventBus eventBus;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  /**
   * Checks whether the chosen game path contains a ForgedAlliance.exe (either directly if the user selected the "bin"
   * directory, or in the "bin" sub folder). If the path is valid, it is stored in the preferences.
   */
  @Subscribe
  public void onGameDirectoryChosenEvent(GameDirectoryChosenEvent event) {
    Path path = event.getPath();
    Optional<CompletableFuture<Path>> future = event.getFuture();

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


  private void detectGamePath() {
    for (Path path : USUAL_GAME_PATHS) {
      if (preferencesService.isGamePathValid(path.resolve("bin"))) {
        onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(path, Optional.empty()));
        return;
      }
    }

    log.warn("Game path could not be detected");
    eventBus.post(new MissingGamePathEvent());
  }

  public void detectAndUpdateGamePath() {
    Path faPath = forgedAlliancePrefs.getInstallationPath();
    if (faPath == null || Files.notExists(faPath)) {
      log.info("Game path is not specified or non-existent, trying to detect");
      detectGamePath();
    }
  }
}
