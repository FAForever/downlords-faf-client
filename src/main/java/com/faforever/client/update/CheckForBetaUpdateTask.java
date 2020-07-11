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

  private static final String PATH_FOR_RELEASE = "/releases";
  private final RestTemplate restTemplate;

  public CheckForBetaUpdateTask(I18n i18n, PreferencesService preferencesService, RestTemplateBuilder restTemplateBuilder) {
    super(i18n, preferencesService);
    restTemplate = restTemplateBuilder.build();
  }

  @Override
  protected UpdateInfo getUpdateInfo(ClientConfiguration clientConfiguration) {
    LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Accept", "application/vnd.github.v3+json");
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    String url = clientConfiguration.getGitHubRepo().getApiUrl() + PATH_FOR_RELEASE;
    ResponseEntity<List<GitHubRelease>> response = restTemplate.exchange(
        url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });

    List<GitHubRelease> responseBody = response.getBody();
    if (responseBody == null || responseBody.isEmpty()) {
      log.warn("No response body from {} ({})", url, response.getStatusCode());
      return null;
    }

    GitHubRelease latestRelease = responseBody.get(0);
    if (!latestRelease.isPrerelease()) {
      return null;
    }

    URL update4jConfigUrl = getUpdate4jAsset(latestRelease)
        .map(GitHubAssets::getBrowserDownloadUrl)
        .orElse(null);

    Configuration newConfig = readConfiguration(update4jConfigUrl);
    if (newConfig == null) {
      return null;
    }

    URL currentVersionConfigUrl = getClass().getResource("/update4j/update4j.xml");
    Configuration oldConfig = currentVersionConfigUrl != null ? readConfiguration(currentVersionConfigUrl) : null;

    long size = calculateUpdateSize(oldConfig, newConfig);

    return new UpdateInfo(
        latestRelease.getName(),
        new ComparableVersion(latestRelease.getTagName()),
        newConfig,
        size,
        latestRelease.getReleaseNotes(),
        true
    );
  }

  @Override
  protected Logger log() {
    return log;
  }

  private Optional<GitHubAssets> getUpdate4jAsset(GitHubRelease latestRelease) {
    return latestRelease.getAssets().stream()
        .filter(asset -> asset.getName().equals("update4j.xml"))
        .findFirst();
  }
}
