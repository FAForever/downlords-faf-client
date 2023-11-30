package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class MissingGamePathNotifier implements InitializingBean {
  private static final Collection<Path> USUAL_GAME_PATHS = Arrays.asList(
      Path.of(System.getenv("ProgramFiles") + "\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + " (x86)\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + " (x86)\\Steam\\steamapps\\common\\supreme commander forged alliance"),
      Path.of(System.getProperty("user.home"), ".steam", "steam", "steamapps", "common",
              "Supreme Commander Forged Alliance"),
      Path.of(System.getenv("ProgramFiles") + "\\Supreme Commander - Forged Alliance")
  );

  private final I18n i18n;
  private final NotificationService notificationService;
  private final LoginService loginService;
  private final PreferencesService preferencesService;
  private final GamePathHandler gamePathHandler;
  private final GameDirectoryRequiredHandler gameDirectoryRequiredHandler;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  @Override
  public void afterPropertiesSet() {
    loginService.loggedInProperty().subscribe(loggedIn -> {
      if (loggedIn) {
        detectAndUpdateGamePath();
      }
    });
  }

  public void onMissingGamePathEvent(boolean immediateUserActionRequired) {
    List<Action> actions = Collections.singletonList(
        new Action(i18n.get("missingGamePath.locate"),
                   chooseEvent -> gameDirectoryRequiredHandler.onChooseGameDirectory(null))
    );
    String notificationText = i18n.get("missingGamePath.notification");

    if (immediateUserActionRequired) {
      notificationService.addNotification(new ImmediateNotification(notificationText, notificationText, Severity.WARN, actions));
    } else {
      notificationService.addNotification(new PersistentNotification(notificationText, Severity.WARN, actions));
    }
  }

  private void detectGamePath() {
    for (Path path : USUAL_GAME_PATHS) {
      if (preferencesService.isValidGamePath(path.resolve("bin"))) {
        gamePathHandler.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(path, null));
        return;
      }
    }

    log.warn("Game path could not be detected");
    onMissingGamePathEvent(false);
  }

  public void detectAndUpdateGamePath() {
    Path faPath = forgedAlliancePrefs.getInstallationPath();
    if (faPath == null || Files.notExists(faPath)) {
      log.info("Game path is not specified or non-existent, trying to detect");
      detectGamePath();
    }
  }
}
