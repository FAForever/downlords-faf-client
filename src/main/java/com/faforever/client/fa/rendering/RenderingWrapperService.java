package com.faforever.client.fa.rendering;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.RenderingBackend;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.GitHubAssets;
import com.faforever.client.update.GitHubRelease;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class RenderingWrapperService {

  private static final Duration GITHUB_API_TIMEOUT = Duration.ofSeconds(30);
  private static final long DOWNLOAD_TIMEOUT_MINUTES = 10;

  private final ClientProperties clientProperties;
  private final DataPrefs dataPrefs;
  private final TaskService taskService;
  private final NotificationService notificationService;
  private final WebClient defaultWebClient;
  private final ObjectFactory<DownloadRenderingWrapperTask> downloadTaskFactory;

  private final Map<RenderingBackend, CompletableFuture<?>> inProgressDownloads = new ConcurrentHashMap<>();

  public boolean ensureWrapperAvailable(RenderingBackend backend) {
    if (!backend.requiresDownload()) {
      return true;
    }

    Path wrapperDir = getWrapperDirectory(backend);
    if (isWrapperInstalled(wrapperDir)) {
      return true;
    }

    log.info("Rendering wrapper for {} not found, downloading...", backend);
    try {
      buildAndSubmitDownloadTask(backend).get(DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
      return true;
    } catch (Exception e) {
      log.error("Failed to download rendering wrapper for {}", backend, e);
      notificationService.addImmediateErrorNotification(e, "rendering.download.failed", backend.name());
      return false;
    }
  }

  public void ensureWrapperAvailableAsync(RenderingBackend backend) {
    if (!backend.requiresDownload()) {
      return;
    }

    Path wrapperDir = getWrapperDirectory(backend);
    if (isWrapperInstalled(wrapperDir)) {
      return;
    }

    if (inProgressDownloads.containsKey(backend)) {
      log.debug("Download already in progress for {}", backend);
      return;
    }

    log.info("Pre-downloading rendering wrapper for {}", backend);
    try {
      CompletableFuture<?> future = buildAndSubmitDownloadTask(backend);
      inProgressDownloads.put(backend, future);
      future.whenComplete((result, throwable) -> {
        inProgressDownloads.remove(backend);
        if (throwable != null) {
          log.warn("Pre-download of rendering wrapper for {} failed", backend, throwable);
        }
      });
    } catch (Exception e) {
      log.warn("Failed to initiate pre-download of rendering wrapper for {}", backend, e);
    }
  }

  public Path getWrapperDirectory(RenderingBackend backend) {
    return dataPrefs.getBinDirectory()
        .resolve("rendering")
        .resolve(backend.name().toLowerCase());
  }

  private boolean isWrapperInstalled(Path wrapperDir) {
    if (!Files.isDirectory(wrapperDir) || !Files.exists(wrapperDir.resolve("d3d9.dll"))) {
      return false;
    }
    Path versionFile = wrapperDir.resolve("version.txt");
    if (!Files.exists(versionFile)) {
      return false;
    }
    try {
      String installedVersion = Files.readString(versionFile).strip();
      return !installedVersion.isEmpty();
    } catch (IOException e) {
      log.warn("Failed to read version file: {}", versionFile, e);
      return false;
    }
  }

  private CompletableFuture<Void> buildAndSubmitDownloadTask(RenderingBackend backend) {
    GitHubRelease release = queryLatestRelease(backend);
    URL assetUrl = resolveAssetUrl(backend, release);

    DownloadRenderingWrapperTask task = downloadTaskFactory.getObject();
    task.setBackend(backend);
    task.setDownloadUrl(assetUrl);
    task.setVersion(release.getTagName());
    task.setWrapperDirectory(getWrapperDirectory(backend));

    return taskService.submitTask(task).getFuture();
  }

  private GitHubRelease queryLatestRelease(RenderingBackend backend) {
    String queryUrl = getQueryLatestVersionUrl(backend);
    return defaultWebClient.get()
        .uri(queryUrl)
        .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
        .retrieve()
        .bodyToMono(GitHubRelease.class)
        .switchIfEmpty(Mono.error(new RuntimeException("No release found for " + backend)))
        .block(GITHUB_API_TIMEOUT);
  }

  private URL resolveAssetUrl(RenderingBackend backend, GitHubRelease release) {
    List<GitHubAssets> assets = release.getAssets();
    if (assets == null || assets.isEmpty()) {
      throw new RuntimeException("Release " + release.getTagName() + " has no assets");
    }
    String archiveFormat = backend.getArchiveFormat();
    return assets.stream()
        .filter(asset -> asset.getName().endsWith("." + archiveFormat))
        .filter(asset -> !asset.getName().contains("-native"))
        .findFirst()
        .map(GitHubAssets::getBrowserDownloadUrl)
        .orElseThrow(() -> new RuntimeException(
            "No matching asset found for " + backend + " in release " + release.getTagName()));
  }

  private String getQueryLatestVersionUrl(RenderingBackend backend) {
    ClientProperties.RenderingWrapper config = clientProperties.getRenderingWrapper();
    return switch (backend) {
      case VULKAN_DXVK -> config.getDxvkQueryLatestVersionUrl();
      case DIRECTX_12 -> config.getD3d9on12QueryLatestVersionUrl();
      default -> throw new IllegalArgumentException("No download URL for " + backend);
    };
  }
}
