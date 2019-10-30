package com.faforever.client.update;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForBetaUpdateTask extends CompletableTask<UpdateInfo> {

  public static final String PATH_FOR_RELEASE = "/releases";
  private final PreferencesService preferencesService;
  private final RestTemplate restTemplate;

  public CheckForBetaUpdateTask(PreferencesService preferencesService, RestTemplateBuilder restTemplateBuilder) {
    super(Priority.LOW);
    this.preferencesService = preferencesService;
    restTemplate = restTemplateBuilder.build();
  }

  @Override
  protected UpdateInfo call() throws Exception {

    LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Accept", "application/vnd.github.v3+json");
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    ClientConfiguration clientConfiguration = preferencesService.getRemotePreferences();
    //Important List<GitHubRelease> needs to stay in  new ParameterizedTypeReference<List<GitHubRelease>>() {}), because of a openjdk compiler bug
    ResponseEntity<List<GitHubRelease>> response = restTemplate.exchange(clientConfiguration.getGitHubRepo().getApiUrl() + PATH_FOR_RELEASE, HttpMethod.GET, entity, new ParameterizedTypeReference<List<GitHubRelease>>() {
    });
    List<GitHubRelease> responseBody = response.getBody();
    if (responseBody == null || responseBody.isEmpty()) {
      return null;
    }
    GitHubRelease latestRelease = responseBody.get(0);
    if (!latestRelease.isPrerelease()) {
      return null;
    }

    GitHubAssets asset;
    if (org.bridj.Platform.isWindows()) {
      asset = getAssetOfFileWithEnding(latestRelease, ".exe");
    } else if (org.bridj.Platform.isLinux()) {
      asset = getAssetOfFileWithEnding(latestRelease, ".tar.gz");
    } else if (org.bridj.Platform.isMacOSX()) {
      asset = getAssetOfFileWithEnding(latestRelease, ".tar.gz");
    } else {
      return null;
    }
    String version = latestRelease.getTagName().substring(1);

    return new UpdateInfo(
        version,
        asset.getName(),
        asset.getBrowserDownloadUrl(),
        asset.getSize(),
        latestRelease.getReleaseNotes(),
        latestRelease.isPrerelease()
    );
  }

  private GitHubAssets getAssetOfFileWithEnding(GitHubRelease latestRelease, String ending) {
    return latestRelease.getAssets().stream()
        .filter(asset -> asset.getName().contains(ending))
        .findFirst()
        .orElseThrow();
  }
}
