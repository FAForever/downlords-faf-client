package com.faforever.client.fa;

import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.util.ProgrammingError;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.ThreadPoolExecutor;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Thread.sleep;

/**
 * Starts flashing the Forged Alliance window whenever a {@link com.faforever.client.fa.relay.event.GameFullEvent} is
 * triggered and stops as soon as the window is focused.
 * Also shows a transient notification.
 */
@Lazy
@Component
public class OnGameFullNotifier {

  @Inject
  PlatformService platformService;
  @Inject
  ThreadPoolExecutor threadPoolExecutor;
  @Inject
  GameService gameService;
  @Inject
  NotificationService notificationService;
  @Inject
  I18n i18n;
  @Inject
  MapService mapService;
  @Inject
  EventBus eventBus;

  @Value("${forgedAlliance.windowTitle}")
  String faWindowTitle;

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onGameFull(GameFullEvent event) {
    threadPoolExecutor.submit(() -> {
      platformService.startFlashingWindow(faWindowTitle);
      while (gameService.isGameRunning() && !faWindowTitle.equals(platformService.getForegroundWindowTitle())) {
        noCatch(() -> sleep(500));
      }
      platformService.stopFlashingWindow(faWindowTitle);
    });

    Game currentGame = gameService.getCurrentGame();
    if (currentGame == null) {
      throw new ProgrammingError("Got a GameFull notification but player is not in a preferences");
    }
    if (faWindowTitle.equals(platformService.getForegroundWindowTitle())) {
      return;
    }

    notificationService.addNotification(new TransientNotification(i18n.get("game.full"), i18n.get("game.full.action"),
        mapService.loadPreview(currentGame.getMapFolderName(), PreviewSize.SMALL),
        v -> platformService.showWindow(faWindowTitle)));
  }
}
