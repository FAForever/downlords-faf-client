package com.faforever.client.ui.preferences;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class GameDirectoryRequiredHandler implements InitializingBean {

  private final EventBus eventBus;
  private final I18n i18n;
  private final PlatformService platformService;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onChooseGameDirectory(GameDirectoryChooseEvent event) {
    platformService.askForPath(i18n.get("missingGamePath.chooserTitle")).ifPresentOrElse(gameDirectory -> {
      log.info("User selected game directory: {}", gameDirectory);
      eventBus.post(new GameDirectoryChosenEvent(gameDirectory, event.getFuture()));
    }, () -> event.getFuture().ifPresent(pathCompletableFuture -> pathCompletableFuture.completeExceptionally(new Exception(i18n.get("missingGamePath.noSelection")))));
  }

}
