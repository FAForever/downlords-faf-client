package com.faforever.client.fa;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.util.ProgrammingError;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Thread.sleep;

/**
 * Starts flashing the Forged Alliance window whenever a {@link com.faforever.client.fa.relay.event.GameFullEvent} is
 * triggered and stops as soon as the window is focused.
 * Also shows a transient notification.
 */
public class OnGameFullNotifier {

  @Resource
  PlatformService platformService;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  GameService gameService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  MapService mapService;
  @Resource
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

    GameInfoBean currentGame = gameService.getCurrentGame();
    if (currentGame == null) {
      throw new ProgrammingError("Got a GameFull notification but player is not in a game");
    }
    if (faWindowTitle.equals(platformService.getForegroundWindowTitle())) {
      return;
    }

    notificationService.addNotification(new TransientNotification(i18n.get("game.full"), i18n.get("game.full.action"),
        mapService.loadPreview(currentGame.getMapFolderName(), PreviewSize.SMALL),
        v -> platformService.showWindow(faWindowTitle)));
  }
}
