package com.faforever.client.ui.preferences;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.i18n.I18n;
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class GameDirectoryRequiredHandler {

  private final I18n i18n;
  private final PlatformService platformService;
  private final GamePathHandler gamePathHandler;

  public void onChooseGameDirectory(CompletableFuture<Path> future) {
    platformService.askForPath(i18n.get("missingGamePath.chooserTitle")).ifPresent(gameDirectory -> {
      log.info("User selected game directory: {}", gameDirectory);
      gamePathHandler.onGameDirectoryChosenEvent(new GameDirectoryChosenEvent(gameDirectory, future));
    });
  }

}
