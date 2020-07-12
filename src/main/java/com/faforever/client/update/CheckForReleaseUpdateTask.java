package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.Configuration;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForReleaseUpdateTask extends AbstractCheckForUpdateTask {

  public CheckForReleaseUpdateTask(I18n i18n, PreferencesService preferencesService, RestTemplateBuilder restTemplateBuilder) {
    super(i18n, preferencesService, restTemplateBuilder);
  }

  @Override
  protected UpdateInfo getUpdateInfo(ClientConfiguration clientConfiguration) {
    Configuration newConfig = readConfiguration(clientConfiguration.getLatestRelease().getUpdate4jConfigUrl());
    if (newConfig == null) {
      return null;
    }

    Optional<Configuration> oldConfiguration = getGitHubReleases(clientConfiguration)
        .flatMap(this::getCurrentConfiguration);

    ReleaseInfo latestRelease = clientConfiguration.getLatestRelease();
    ComparableVersion version = latestRelease.getVersion();

    return new UpdateInfo(
        latestRelease.getVersion().toString(),
        version,
        oldConfiguration,
        newConfig,
        calculateUpdateSize(oldConfiguration, newConfig),
        latestRelease.getReleaseNotesUrl(),
        false
    );
  }
}
