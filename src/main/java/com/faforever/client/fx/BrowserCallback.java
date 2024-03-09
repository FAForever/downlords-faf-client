package com.faforever.client.fx;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.navigation.NavigationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class BrowserCallback implements InitializingBean {

  private final PlatformService platformService;
  private final ClientProperties clientProperties;
  private final NavigationHandler navigationHandler;

  private Pattern replayUrlPattern;

  @Override
  public void afterPropertiesSet() {
    String urlFormat = clientProperties.getVault().getReplayDownloadUrlFormat();
    String[] splitFormat = urlFormat.split("%s");
    replayUrlPattern = Pattern.compile(Pattern.quote(splitFormat[0]) + "(\\d+)" + Pattern.compile(splitFormat.length == 2 ? splitFormat[1] : ""));
  }

  /**
   * Called from JavaScript when user clicked a URL.
   */
  @SuppressWarnings("unused")
  public void openUrl(String url) {
    Matcher replayUrlMatcher = replayUrlPattern.matcher(url);
    if (!replayUrlMatcher.matches()) {
      platformService.showDocument(url);
      return;
    }

    String replayId = replayUrlMatcher.group(1);
    navigationHandler.navigateTo(new ShowReplayEvent(Integer.parseInt(replayId)));
  }
}
