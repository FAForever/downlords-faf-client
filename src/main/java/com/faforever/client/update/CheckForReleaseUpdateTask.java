package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForReleaseUpdateTask extends AbstractCheckForUpdateTask {

  public CheckForReleaseUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(i18n, preferencesService);
  }

  @Override
  protected Logger log() {
    return log;
  }
}
