package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
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

import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForBetaUpdateTask extends CompletableTask<UpdateInfo> {

  public static final String PATH_FOR_RELEASE = "/releases";
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final RestTemplate restTemplate;

  public CheckForBetaUpdateTask(PreferencesService preferencesService,
                                I18n i18n,
                                RestTemplateBuilder restTemplateBuilder) {
    super(Priority.LOW);
    this.preferencesService = preferencesService;
    this.i18n = i18n;
    restTemplate = restTemplateBuilder.build();
  }

  @Override
  protected UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    log.info("Checking for client update (pre-release channel)");

    LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Accept", "application/vnd.github.v3+json");
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    ClientConfiguration clientConfiguration = preferencesService.getRemotePreferences();
    //Important List<GitHubRelease> needs to stay in  new ParameterizedTypeReference<List<GitHubRelease>>() {}), because of a openjdk compiler bug
    ResponseEntity<List<GitHubRelease>> response = restTemplate.exchange(clientConfiguration.getGitHubRepo().getApiUrl() + PATH_FOR_RELEASE, HttpMethod.GET, entity, new ParameterizedTypeReference<List<GitHubRelease>>() {
    });
    List<GitHubRelease> releases = response.getBody();

    if (releases == null) {
      return null;
    }

    GitHubRelease latestRelease = releases.stream()
        .filter(release -> Version.followsSemverPattern(release.getTagName()))
        .max(Comparator.comparing(release -> new ComparableVersion(Version.removePrefix(release.getTagName()))))
        .orElse(null);

    if (latestRelease == null) {
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
      log.warn("Could not determine operating system");
      return null;
    }

    return new UpdateInfo(
        Version.removePrefix(latestRelease.getTagName()),
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
