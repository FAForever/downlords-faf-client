package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
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
    if (!latestRelease.isPrerelease()) {
      return Optional.empty();
    }

    URL update4jConfigUrl = getUpdate4jConfigAsset(latestRelease)
        .map(GitHubAsset::getBrowserDownloadUrl)
        .orElse(null);

    Configuration newConfig = readConfiguration(update4jConfigUrl);
    if (newConfig == null) {
      return Optional.empty();
    }

    Optional<Configuration> oldConfiguration = getCurrentConfiguration(gitHubReleases.get());

    long size = calculateUpdateSize(oldConfiguration, newConfig);

    return Optional.of(new UpdateInfo(
        latestRelease.getName(),
        new ComparableVersion(latestRelease.getTagName()),
        oldConfiguration,
        newConfig,
        size,
        latestRelease.getReleaseNotes(),
        true
    ));
  }
}
