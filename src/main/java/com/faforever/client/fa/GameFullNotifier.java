package com.faforever.client.fa;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Starts flashing the Forged Alliance window whenever a {@link com.faforever.client.fa.relay.event.GameFullEvent} is
 * triggered and stops as soon as the window is focused. Also shows a transient notification.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class GameFullNotifier {

  private final PlatformService platformService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final MapService mapService;
  private final GameRunner gameRunner;
  private final ClientProperties clientProperties;

  private final Timer flashingWindowTimer = new Timer("Flashing Window", true);

  public void onGameFull() {
    if (!gameRunner.isRunning()) {
      throw new ProgrammingError("Got a GameFull notification but player is not in a game");
    }
    Long processId = gameRunner.getRunningProcessId();
    GameBean runningGame = gameRunner.getRunningGame();

    if (platformService.getFocusedWindowProcessId() == processId) {
      log.debug("Game lobby window is focused. No need notify the user");
      return;
    }

    String faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
    platformService.startFlashingWindow(faWindowTitle, processId);
    flashingWindowTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (!gameRunner.isRunning() || platformService.isWindowFocused(faWindowTitle, processId)) {
          platformService.stopFlashingWindow(faWindowTitle, processId);
          cancel();
        }
      }
    }, 500, 500);

    notificationService.addNotification(new TransientNotification(i18n.get("game.full"), i18n.get("game.full.action"),
                                                                  mapService.loadPreview(runningGame.getMapFolderName(),
                                                                                         PreviewSize.SMALL), () -> {
          if (platformService.isWindowFocused(faWindowTitle)) {
            // Switching to the game lobby window from replay window may not work correctly (no interaction) for reasons:
            // 1) The game has full screen mode
            // 2) A resolution in the game and in the screen is different
            platformService.minimizeFocusedWindow();
          }
          platformService.focusWindow(faWindowTitle, processId);
        }));
  }
}
