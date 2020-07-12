package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
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
import org.update4j.FileMetadata;
import org.update4j.OS;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public abstract class AbstractCheckForUpdateTask extends CompletableTask<UpdateInfo> {

  private static final String PATH_FOR_RELEASE = "/releases";

  private final I18n i18n;
  private final PreferencesService preferencesService;
  private final RestTemplate restTemplate;

  public AbstractCheckForUpdateTask(I18n i18n, PreferencesService preferencesService, RestTemplateBuilder restTemplateBuilder) {
    super(Priority.LOW);
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    restTemplate = restTemplateBuilder.build();
  }

  @Override
  protected final UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    log.info("Checking for client update");

    ClientConfiguration clientConfiguration = preferencesService.getRemotePreferences();

    return getUpdateInfo(clientConfiguration);
  }

  abstract protected UpdateInfo getUpdateInfo(ClientConfiguration clientConfiguration);

  protected long calculateUpdateSize(Optional<Configuration> oldConfig, Configuration newConfig) {
    Map<String, FileMetadata> filesToDownload = newConfig.getFiles().stream()
        .collect(Collectors.toMap(this::key, Function.identity()));

    oldConfig.ifPresent(configuration -> configuration.getFiles().stream()
        .collect(Collectors.toMap(this::key, Function.identity()))
        .forEach((checksum, fileMetadata) -> filesToDownload.remove(key(fileMetadata))));

    return filesToDownload.values().stream()
        .filter(file -> file.getOs() == null || file.getOs() == OS.CURRENT)
        .mapToLong(FileMetadata::getSize)
        .sum();
  }

  @NotNull
  private String key(FileMetadata metadata) {
    return metadata.getPath().toString() + metadata.getChecksum();
  }

  protected Configuration readConfiguration(URL update4jConfigUrl) {
    Configuration newConfig;
    try (Reader reader = new InputStreamReader(update4jConfigUrl.openStream())) {
      newConfig = Configuration.read(reader);
    } catch (Exception e) {
      log.warn("Error while reading config from {}", update4jConfigUrl, e);
      return null;
    }
    return newConfig;
  }

  protected Optional<Configuration> getCurrentConfiguration(List<GitHubRelease> responseBody) {
    return responseBody.stream()
        .filter(gitHubRelease -> new ComparableVersion(gitHubRelease.getTagName()).equals(Version.getCurrentVersion()))
        .findFirst()
        .flatMap(this::getUpdate4jConfigAsset)
        .map(GitHubAsset::getBrowserDownloadUrl)
        .map(this::readConfiguration);
  }

  protected Optional<GitHubAsset> getUpdate4jConfigAsset(GitHubRelease latestRelease) {
    return latestRelease.getAssets().stream()
        .filter(asset -> asset.getName().equals("update4j.xml"))
        .findFirst();
  }

  protected Optional<List<GitHubRelease>> getGitHubReleases(ClientConfiguration clientConfiguration) {
    LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Accept", "application/vnd.github.v3+json");
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    String url = clientConfiguration.getGitHubRepo().getApiUrl() + PATH_FOR_RELEASE;
    ResponseEntity<List<GitHubRelease>> response = restTemplate.exchange(
        url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });

    if (!response.getStatusCode().is2xxSuccessful()) {
      log.warn("No response body from {} ({})", url, response.getStatusCode());
    }
    return Optional.ofNullable(response.getBody());
  }
}
