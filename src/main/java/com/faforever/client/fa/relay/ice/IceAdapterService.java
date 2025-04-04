package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.map.generator.OutdatedVersionException;
import com.faforever.client.map.generator.UnsupportedVersionException;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.os.OsUnknown;
import com.faforever.client.os.OsWindows;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.GitHubRelease;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Service
@Slf4j
@RequiredArgsConstructor
public class IceAdapterService {
  public static final String ICE_ADAPTER_EXECUTABLE_FILENAME = "faf-pioneer-%s-%s";

  private final OperatingSystem operatingSystem;
  private final ClientProperties clientProperties;
  private final WebClient defaultWebClient;
  private final DataPrefs dataPrefs;
  private final ObjectFactory<DownloadIceAdapterTask> downloadIceAdapterTaskFactory;
  private final TaskService taskService;

  private ComparableVersion defaultVersion;

  private Mono<ComparableVersion> queryMaxSupportedVersion() {
    ComparableVersion minVersion = new ComparableVersion(
        String.valueOf(clientProperties.getIceAdapter().getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(
        String.valueOf(clientProperties.getIceAdapter().getMaxSupportedMajorVersion() + 1));

    return defaultWebClient.get()
                           .uri(clientProperties.getIceAdapter().getQueryVersionsUrl())
                           .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
                           .retrieve()
                           .bodyToFlux(GitHubRelease.class)
                           .map(release -> new ComparableVersion(release.getTagName()))
                           .filter(version -> version.compareTo(maxVersion) < 0 && minVersion.compareTo(version) < 0)
                           .sort(Comparator.naturalOrder())
                           .last()
                           .switchIfEmpty(Mono.error(new RuntimeException("No valid ice adapter found")));
  }

  public Mono<Void> downloadfNecessary(ComparableVersion version) {
    ComparableVersion minVersion = new ComparableVersion(
        String.valueOf(clientProperties.getIceAdapter().getMinSupportedMajorVersion()));
    ComparableVersion maxVersion = new ComparableVersion(
        String.valueOf(clientProperties.getIceAdapter().getMaxSupportedMajorVersion() + 1));
    if (version.compareTo(maxVersion) >= 0) {
      return Mono.error(new UnsupportedVersionException("New version not supported"));
    }
    if (version.compareTo(minVersion) < 0) {
      return Mono.error(new OutdatedVersionException("Old Version not supported"));
    }
    Path generatorExecutablePath = getExecutablePath(version);

    if (!Files.exists(generatorExecutablePath)) {
      log.info("Downloading ice adapter version: {}", version);
      DownloadIceAdapterTask downloadIceAdapterTask = downloadIceAdapterTaskFactory.getObject();
      downloadIceAdapterTask.setVersion(version);
      return taskService.submitTask(downloadIceAdapterTask).getMono();
    } else {
      log.info("Found ice adapter version: {}", version);
      return Mono.empty();
    }
  }

  @NotNull
  public Path getExecutablePath() {
    if(defaultVersion == null) {
      throw new IllegalStateException("Current version not set");
    }

    return dataPrefs.getIceAdapterDirectory()
                    .resolve(String.format(ICE_ADAPTER_EXECUTABLE_FILENAME, defaultVersion, getFileSuffix()));
  }

  @NotNull
  public Path getExecutablePath(ComparableVersion defaultVersion) {
    return dataPrefs.getIceAdapterDirectory()
                    .resolve(String.format(ICE_ADAPTER_EXECUTABLE_FILENAME, defaultVersion, getFileSuffix()));
  }

  public String getFileSuffix() {
    return switch (operatingSystem) {
      case OsPosix osPosix -> clientProperties.getIceAdapter().getLinuxSuffix();
      case OsUnknown osUnknown -> throw new IllegalStateException("faf-pioneer needs specific OS");
      case OsWindows osWindows -> clientProperties.getIceAdapter().getWindowsSuffix();
    };
  }

  @Cacheable(value = CacheNames.ICE_ADAPTER, sync = true)
  public Mono<Void> getNewest() {
    return queryMaxSupportedVersion().doOnNext(newVersion -> defaultVersion = newVersion)
                                     .flatMap(this::downloadfNecessary);
  }
}
