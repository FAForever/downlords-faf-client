package com.faforever.client.ui.preferences;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.i18n.I18n;
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

  public CompletableFuture<Path> onChooseGameDirectory() {
    return platformService.askForPath(i18n.get("missingGamePath.chooserTitle"))
                          .thenCompose(
                              possiblePath -> gamePathHandler.onGameDirectoryChosenEvent(possiblePath.orElse(null)));
  }

  ;

}
