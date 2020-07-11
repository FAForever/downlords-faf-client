package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.Configuration;

import java.net.URL;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForReleaseUpdateTask extends AbstractCheckForUpdateTask {

  public CheckForReleaseUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(i18n, preferencesService);
  }

  @Override
  protected UpdateInfo getUpdateInfo(ClientConfiguration clientConfiguration) {
    Configuration newConfig = readConfiguration(clientConfiguration.getLatestRelease().getUpdate4jConfigUrl());
    if (newConfig == null) {
      return null;
    }

    URL currentVersionConfigUrl = getClass().getResource("/update4j/update4j.xml");
    Configuration oldConfig = currentVersionConfigUrl != null ? readConfiguration(currentVersionConfigUrl) : null;

    ReleaseInfo latestRelease = clientConfiguration.getLatestRelease();
    ComparableVersion version = latestRelease.getVersion();

    return new UpdateInfo(
        latestRelease.getVersion().toString(),
        version,
        newConfig,
        calculateUpdateSize(oldConfig, newConfig),
        latestRelease.getReleaseNotesUrl(),
        false
    );
  }

  @Override
  protected Logger log() {
    return log;
  }
}
