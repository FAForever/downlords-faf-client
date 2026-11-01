package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.GitHubRelease;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class IceService {

  private static final String ICE_ADAPTER_JAR_FILENAME_FORMAT = "faf-ice-adapter-%s.jar";
  private static final Pattern ICE_ADAPTER_JAR_FILENAME_PATTERN = Pattern.compile("faf-ice-adapter-(.+)\\.jar");

  private final ClientProperties clientProperties;
  private final DataPrefs dataPrefs;
  private final TaskService taskService;
  private final WebClient defaultWebClient;
  private final ObjectFactory<DownloadIceAdapterTask> downloadIceAdapterTaskFactory;

  /**
   * Resolves the jar of the latest ICE adapter release, downloading it if necessary. If the latest version cannot be
   * determined or downloaded (e.g. GitHub is unreachable or rate limited), falls back to the newest locally available
   * jar.
   */
  @Cacheable(value = CacheNames.ICE_ADAPTER, sync = true)
  public Mono<Path> getNewestIceAdapter() {
    return queryLatestVersion().flatMap(version -> {
      Path iceAdapterJar = getIceAdapterJarPath(version);
      return downloadIceAdapterIfNecessary(version, iceAdapterJar).thenReturn(iceAdapterJar);
    }).onErrorResume(throwable -> findNewestLocalIceAdapterJar().map(iceAdapterJar -> {
      log.warn("Could not get latest ICE adapter, falling back to `{}`", iceAdapterJar, throwable);
      return Mono.just(iceAdapterJar);
    }).orElseGet(() -> Mono.error(throwable)));
  }

  @VisibleForTesting
  Mono<String> queryLatestVersion() {
    return defaultWebClient.get()
                           .uri(clientProperties.getIceAdapter().getQueryLatestVersionUrl())
                           .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
                           .retrieve()
                           .bodyToMono(GitHubRelease.class)
                           .map(GitHubRelease::getTagName)
                           .switchIfEmpty(Mono.error(new IllegalStateException("No ICE adapter release found")));
  }

  @VisibleForTesting
  Optional<Path> findNewestLocalIceAdapterJar() {
    Path iceAdapterDirectory = dataPrefs.getIceAdapterDirectory();
    if (!Files.isDirectory(iceAdapterDirectory)) {
      return Optional.empty();
    }

    try (Stream<Path> files = Files.list(iceAdapterDirectory)) {
      return files.filter(Files::isRegularFile)
                  .filter(file -> ICE_ADAPTER_JAR_FILENAME_PATTERN.matcher(file.getFileName().toString()).matches())
                  .max(Comparator.comparing(IceService::getIceAdapterJarVersion));
    } catch (IOException e) {
      log.warn("Could not list ICE adapter directory `{}`", iceAdapterDirectory, e);
      return Optional.empty();
    }
  }

  private static ComparableVersion getIceAdapterJarVersion(Path iceAdapterJar) {
    Matcher matcher = ICE_ADAPTER_JAR_FILENAME_PATTERN.matcher(iceAdapterJar.getFileName().toString());
    return new ComparableVersion(matcher.matches() ? matcher.group(1) : "");
  }

  @VisibleForTesting
  Path getIceAdapterJarPath(String version) {
    return dataPrefs.getIceAdapterDirectory().resolve(String.format(ICE_ADAPTER_JAR_FILENAME_FORMAT, version));
  }

  @VisibleForTesting
  Mono<Void> downloadIceAdapterIfNecessary(String version, Path iceAdapterJar) {
    if (Files.exists(iceAdapterJar)) {
      log.info("Found ICE adapter version: {}", version);
      return Mono.empty();
    }

    log.info("Downloading ICE adapter version: {}", version);
    DownloadIceAdapterTask downloadIceAdapterTask = downloadIceAdapterTaskFactory.getObject();
    downloadIceAdapterTask.setVersion(version);
    downloadIceAdapterTask.setTargetFile(iceAdapterJar);
    return taskService.submitTask(downloadIceAdapterTask).getMono();
  }
}
