package com.faforever.client.ui.preferences;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.i18n.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class GameDirectoryRequiredHandler {

  private final I18n i18n;
  private final PlatformService platformService;
  private final GamePathHandler gamePathHandler;



}
