package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Mono;

import java.util.Comparator;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForBetaUpdateTask extends CompletableTask<UpdateInfo> {

  public static final String PATH_FOR_RELEASE = "/releases";
  private final OperatingSystem operatingSystem;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final WebClient webClient;

  public CheckForBetaUpdateTask(OperatingSystem operatingSystem, PreferencesService preferencesService,
                                I18n i18n,
                                Builder webClientBuilder) {
    super(Priority.LOW);
    this.operatingSystem = operatingSystem;
    this.preferencesService = preferencesService;
    this.i18n = i18n;
    webClient = webClientBuilder.build();
  }

  @Override
  protected UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    log.info("Checking for client update (pre-release channel)");

    return preferencesService.getRemotePreferencesAsync().thenApply(clientConfiguration -> webClient.get()
        .uri(clientConfiguration.getGitHubRepo().getApiUrl() + PATH_FOR_RELEASE)
        .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
        .retrieve()
        .bodyToFlux(GitHubRelease.class)
        .filter(release -> Version.followsSemverPattern(release.getTagName()))
        .sort(Comparator.comparing(release -> new ComparableVersion(Version.removePrefix(release.getTagName()))))
        .last()
        .flatMap(release -> {
          try {
            GitHubAssets asset = getAssetOfFileWithEnding(release, operatingSystem.getGithubAssetFileEnding());
            return Mono.just(new UpdateInfo(
                Version.removePrefix(release.getTagName()),
                asset.getName(),
                asset.getBrowserDownloadUrl(),
                asset.getSize(),
                release.getReleaseNotes(),
                release.isPrerelease()
            ));
          } catch (NotImplementedException e) {
            log.warn("Could not determine operating system");
            return Mono.empty();
          }
        }).block()).join();
  }

  private GitHubAssets getAssetOfFileWithEnding(GitHubRelease latestRelease, String ending) {
    return latestRelease.getAssets().stream()
        .filter(asset -> asset.getName().contains(ending))
        .findFirst()
        .orElseThrow();
  }
}
