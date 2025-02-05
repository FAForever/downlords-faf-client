package com.faforever.client.logging.analysis;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogAnalyzerService {
  private static final String GAME_MINIMIZED_TRACE = "info: Minimized true";
  private static final String SND_WARNING_TRACE = "warning: SND";
  private static final String SND_XACT_TRACE = "XACT";

  private final I18n i18n;
  private final ClientProperties clientProperties;
  private final PlatformService platformService;

  @NotNull
  public Map<String, Action> analyzeLogContents(final String logContents) {
    final Map<String, Action> analysisResult = new HashMap<>();

    if (StringUtils.contains(logContents, GAME_MINIMIZED_TRACE)) {
      analysisResult.put(i18n.get("game.log.analysis.minimized"), null);
    }

    if (StringUtils.contains(logContents, SND_WARNING_TRACE) && StringUtils.contains(logContents, SND_XACT_TRACE)) {
      final String moreInfoButtonCaption = i18n.get("game.log.analysis.moreInfoBtn");
      final Action openSoundHelpAction = new Action(moreInfoButtonCaption, () -> platformService.showDocument(
          clientProperties.getLinks().get("linksSoundIssues")));

      analysisResult.put(i18n.get("game.log.analysis.snd", moreInfoButtonCaption), openSoundHelpAction);
    }

    return Collections.unmodifiableMap(analysisResult);
  }
}
