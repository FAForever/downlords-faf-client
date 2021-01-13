package com.faforever.client.fa;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.events.TransientNotificationEvent;
import com.faforever.client.util.ProgrammingError;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.Thread.sleep;

/**
 * Starts flashing the Forged Alliance window whenever a {@link com.faforever.client.fa.relay.event.GameFullEvent} is
 * triggered and stops as soon as the window is focused. Also shows a transient notification.
 */

@Component
public class OnGameFullNotifier implements InitializingBean {

  private final PlatformService platformService;
  private final ExecutorService executorService;
  private final I18n i18n;
  private final MapService mapService;
  private final EventBus eventBus;
  private final GameService gameService;
  private final ClientProperties clientProperties;
  private final String faWindowTitle;

  @Inject
  public OnGameFullNotifier(PlatformService platformService, ExecutorService executorService,
                            I18n i18n,
                            MapService mapService,
                            EventBus eventBus,
                            GameService gameService,
                            ClientProperties clientProperties) {
    this.platformService = platformService;
    this.executorService = executorService;
    this.i18n = i18n;
    this.mapService = mapService;
    this.eventBus = eventBus;
    this.gameService = gameService;
    this.clientProperties = clientProperties;
    this.faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle();
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onGameFull(GameFullEvent event) {
    executorService.execute(() -> {
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

    eventBus.post(new TransientNotificationEvent(i18n.get("game.full"), i18n.get("game.full.action"),
        mapService.loadPreview(currentGame.getMapFolderName(), PreviewSize.SMALL),
        v -> platformService.focusWindow(faWindowTitle)));
  }
}
