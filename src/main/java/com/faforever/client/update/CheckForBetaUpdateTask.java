package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.Configuration;

import java.net.URL;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForBetaUpdateTask extends AbstractCheckForUpdateTask {

  public CheckForBetaUpdateTask(I18n i18n, PreferencesService preferencesService, RestTemplateBuilder restTemplateBuilder) {
    super(i18n, preferencesService, restTemplateBuilder);
  }

  @Override
  protected Optional<UpdateInfo> getUpdateInfo(ClientConfiguration clientConfiguration) {
    Optional<List<GitHubRelease>> gitHubReleases = getGitHubReleases(clientConfiguration);
    if (gitHubReleases.isEmpty() || gitHubReleases.get().isEmpty()) {
      return Optional.empty();
    }

    GitHubRelease latestRelease = gitHubReleases.get().get(0);
    if (!latestRelease.isPreRelease()) {
      return Optional.empty();
    }

    URL update4jConfigUrl = getUpdate4jConfigAsset(latestRelease)
        .map(GitHubAsset::getBrowserDownloadUrl)
        .orElse(null);

    Configuration newConfig = readConfiguration(update4jConfigUrl);
    if (newConfig == null) {
      return Optional.empty();
    }

    // FIXME read from local disk
    Optional<Configuration> oldConfiguration = getCurrentConfiguration(gitHubReleases.get());

    long size = calculateUpdateSize(oldConfiguration, newConfig);

    return Optional.of(new UpdateInfo(
        latestRelease.getName(),
        versionFromTag(latestRelease.getTagName()),
        oldConfiguration,
        newConfig,
        size,
        latestRelease.getReleaseNotes(),
        true
    ));
  }
}
