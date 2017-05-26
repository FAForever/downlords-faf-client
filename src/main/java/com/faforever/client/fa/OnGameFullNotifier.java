package com.faforever.client.fa;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.util.ProgrammingError;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.Executor;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Thread.sleep;

/**
 * Starts flashing the Forged Alliance window whenever a {@link com.faforever.client.fa.relay.event.GameFullEvent} is
 * triggered and stops as soon as the window is focused. Also shows a transient notification.
 */
@Component
public class OnGameFullNotifier {

  private final PlatformService platformService;
  private final Executor executor;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final MapService mapService;
  private final EventBus eventBus;
  private final GameService gameService;
  private final String faWindowTitle;

  @Inject
  public OnGameFullNotifier(PlatformService platformService, Executor executor, NotificationService notificationService,
                            I18n i18n, MapService mapService, EventBus eventBus, ClientProperties clientProperties,
                            GameService gameService) {
    this.platformService = platformService;
    this.executor = executor;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.mapService = mapService;
    this.eventBus = eventBus;
    this.faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
    this.gameService = gameService;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onGameFull(GameFullEvent event) {
    executor.execute(() -> {
      platformService.startFlashingWindow(faWindowTitle);
      while (gameService.isGameRunning() && !platformService.isWindowFocused(faWindowTitle)) {
        noCatch(() -> sleep(500));
      }
      platformService.stopFlashingWindow(faWindowTitle);
    });

    Game currentGame = gameService.getCurrentGame();
    if (currentGame == null) {
      throw new ProgrammingError("Got a GameFull notification but player is not in a game");
    }
    if (platformService.isWindowFocused(faWindowTitle)) {
      return;
    }

    notificationService.addNotification(new TransientNotification(i18n.get("game.full"), i18n.get("game.full.action"),
        mapService.loadPreview(currentGame.getMapFolderName(), PreviewSize.SMALL),
        v -> platformService.focusWindow(faWindowTitle)));
  }
}
