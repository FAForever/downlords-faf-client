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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class RenderingWrapperService {

  private final ClientProperties clientProperties;
  private final DataPrefs dataPrefs;
  private final TaskService taskService;
  private final NotificationService notificationService;
  private final WebClient defaultWebClient;
  private final ObjectFactory<DownloadRenderingWrapperTask> downloadTaskFactory;

  /**
   * Ensures the wrapper DLLs for the given backend are available.
   * Returns true if the wrapper is ready, false if download failed.
   * Blocks the calling thread while downloading (with progress shown in UI).
   */
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
      downloadWrapper(backend);
      return true;
    } catch (Exception e) {
      log.error("Failed to download rendering wrapper for {}", backend, e);
      notificationService.addImmediateErrorNotification(e, "rendering.download.failed", backend.name());
      return false;
    }
  }

  /**
   * Triggers a non-blocking download of the wrapper DLLs for the given backend.
   * Used for pre-downloading when the user changes settings.
   */
  public void ensureWrapperAvailableAsync(RenderingBackend backend) {
    if (!backend.requiresDownload()) {
      return;
    }

    Path wrapperDir = getWrapperDirectory(backend);
    if (isWrapperInstalled(wrapperDir)) {
      return;
    }

    log.info("Pre-downloading rendering wrapper for {}", backend);
    try {
      GitHubRelease release = queryLatestRelease(backend);
      URL assetUrl = resolveAssetUrl(backend, release);

      DownloadRenderingWrapperTask task = downloadTaskFactory.getObject();
      task.setBackend(backend);
      task.setDownloadUrl(assetUrl);
      task.setVersion(release.getTagName());

      taskService.submitTask(task).getFuture().exceptionally(throwable -> {
        log.warn("Pre-download of rendering wrapper for {} failed", backend, throwable);
        return null;
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
    return Files.isDirectory(wrapperDir)
        && Files.exists(wrapperDir.resolve("d3d9.dll"));
  }

  private void downloadWrapper(RenderingBackend backend) {
    GitHubRelease release = queryLatestRelease(backend);
    URL assetUrl = resolveAssetUrl(backend, release);

    DownloadRenderingWrapperTask task = downloadTaskFactory.getObject();
    task.setBackend(backend);
    task.setDownloadUrl(assetUrl);
    task.setVersion(release.getTagName());

    taskService.submitTask(task).getFuture().join();
  }

  private GitHubRelease queryLatestRelease(RenderingBackend backend) {
    String queryUrl = getQueryLatestVersionUrl(backend);
    return defaultWebClient.get()
        .uri(queryUrl)
        .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
        .retrieve()
        .bodyToMono(GitHubRelease.class)
        .switchIfEmpty(Mono.error(new RuntimeException("No release found for " + backend)))
        .block();
  }

  private URL resolveAssetUrl(RenderingBackend backend, GitHubRelease release) {
    List<GitHubAssets> assets = release.getAssets();
    if (assets == null || assets.isEmpty()) {
      throw new RuntimeException("Release " + release.getTagName() + " has no assets");
    }
    String archiveFormat = backend.getArchiveFormat();
    return assets.stream()
        .filter(asset -> asset.getName().endsWith("." + archiveFormat))
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
