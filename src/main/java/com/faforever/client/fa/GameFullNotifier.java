package com.faforever.client.fa;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;

/**
 * Starts flashing the Forged Alliance window whenever a {@link com.faforever.client.fa.relay.event.GameFullEvent} is
 * triggered and stops as soon as the window is focused. Also shows a transient notification.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class GameFullNotifier implements InitializingBean {

  private final PlatformService platformService;
  private final ExecutorService executorService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final MapService mapService;
  private final GameService gameService;
  private final ClientProperties clientProperties;

  private String faWindowTitle;
  private long processId;

  @Override
  public void afterPropertiesSet() {
    faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
  }

  public void onGameFull() {
    GameBean currentGame = gameService.getCurrentGame();
    if (currentGame == null) {
      throw new ProgrammingError("Got a GameFull notification but player is not in a game");
    }

    processId = gameService.getRunningProcessId();

    if (platformService.getFocusedWindowProcessId() == processId) {
      log.debug("Game lobby window is focused. No need notify the user");
      return;
    }

    executorService.execute(() -> {
      platformService.startFlashingWindow(faWindowTitle, processId);
      while (gameService.isGameRunning() && !platformService.isWindowFocused(faWindowTitle, processId)) {
        try {
          sleep(500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      platformService.stopFlashingWindow(faWindowTitle, processId);
    });

    notificationService.addNotification(new TransientNotification(i18n.get("game.full"), i18n.get("game.full.action"),
        mapService.loadPreview(currentGame.getMapFolderName(), PreviewSize.SMALL),
        v -> {
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
