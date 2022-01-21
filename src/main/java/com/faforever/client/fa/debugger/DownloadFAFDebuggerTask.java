package com.faforever.client.fa.debugger;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.ForgedAllianceService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.update.GitHubRelease;
import com.faforever.commons.io.ByteCopier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class DownloadFAFDebuggerTask extends CompletableTask<Void> {

  private final ClientProperties clientProperties;
  private final I18n i18n;
  private final PlatformService platformService;
  private final ForgedAllianceService forgedAllianceService;
  private final WebClient webClient;

  @Inject
  public DownloadFAFDebuggerTask(ClientProperties clientProperties, I18n i18n, PlatformService platformService, ForgedAllianceService forgedAllianceService,
                                 WebClient.Builder webClientBuilder) {
    super(Priority.HIGH);

    this.clientProperties = clientProperties;
    this.i18n = i18n;
    this.platformService = platformService;
    this.forgedAllianceService = forgedAllianceService;
    this.webClient = webClientBuilder.build();
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("game.fa.downloadDebugger.title"));

    String version = webClient.get()
        .uri(clientProperties.getFafDebugger().getQueryLatestVersionUrl())
        .accept(MediaType.parseMediaType("application/vnd.github.v3+json"))
        .retrieve()
        .bodyToMono(GitHubRelease.class)
        .map(GitHubRelease::getTagName)
        .switchIfEmpty(Mono.error(new RuntimeException("No valid debugger version found")))
        .block();

    URL url = new URL(String.format(clientProperties.getFafDebugger().getDownloadUrlFormat(), version));

    URLConnection urlConnection = url.openConnection();

    Path targetFile = forgedAllianceService.getDebuggerExecutablePath();
    Path tempFile = Files.createTempFile(targetFile.getParent(), "debugger", null);

    ResourceLocks.acquireDownloadLock();
    try (InputStream inputStream = url.openStream(); OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(urlConnection.getContentLength())
          .listener(this::updateProgress)
          .copy();

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      ResourceLocks.freeDownloadLock();
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }

    platformService.setUnixExecutableAndWritableBits(targetFile);

    return null;
  }
}