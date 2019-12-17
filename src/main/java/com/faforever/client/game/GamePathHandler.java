package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

@Component
public class GamePathHandler implements InitializingBean {
  private static final Collection<Path> USUAL_GAME_PATHS = Arrays.asList(
      Paths.get(System.getenv("ProgramFiles") + "\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Paths.get(System.getenv("ProgramFiles") + " (x86)\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Paths.get(System.getenv("ProgramFiles") + "\\Steam\\steamapps\\common\\supreme commander forged alliance"),
      Paths.get(System.getenv("ProgramFiles") + "\\Supreme Commander - Forged Alliance")
  );
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final PreferencesService preferencesService;

  public GamePathHandler(NotificationService notificationService, I18n i18n, EventBus eventBus, PreferencesService preferencesService) {
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.preferencesService = preferencesService;

  }

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

    if (path == null || !Files.isDirectory(path)) {
      notificationService.addNotification(new ImmediateNotification(i18n.get("gameChosen.invalidPath"), i18n.get("gameChosen.couldNotLocatedGame"), Severity.WARN));
      return;
    }

    if (!Files.isRegularFile(path.resolve(PreferencesService.FORGED_ALLIANCE_EXE)) && !Files.isRegularFile(path.resolve(PreferencesService.SUPREME_COMMANDER_EXE))) {
      onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(path.resolve("bin")));
      return;
    }

    // At this point, path points to the "bin" directory
    Path gamePath = path.getParent();

    logger.info("Found game path at {}", gamePath);
    preferencesService.getPreferences().getForgedAlliance().setInstallationPath(gamePath);
    preferencesService.storeInBackground();
  }


  private void detectGamePath() {
    for (Path path : USUAL_GAME_PATHS) {
      if (preferencesService.isGamePathValid(path.resolve("bin"))) {
        onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(path));
        return;
      }
    }

    logger.info("Game path could not be detected");
    eventBus.post(new MissingGamePathEvent());
  }

  public void detectAndUpdateGamePath() {
    Path faPath = preferencesService.getPreferences().getForgedAlliance().getInstallationPath();
    if (faPath == null || Files.notExists(faPath)) {
      logger.info("Game path is not specified or non-existent, trying to detect");
      detectGamePath();
    }
  }
}
