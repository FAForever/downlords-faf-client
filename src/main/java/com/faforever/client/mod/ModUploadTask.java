package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Validator;
import com.faforever.commons.io.ByteCountListener;
import com.faforever.commons.io.Zipper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import static com.faforever.commons.io.Bytes.formatSize;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ModUploadTask extends CompletableTask<Void> {

  static final String MOD_UPLOAD_START_API_GET = "/mods/upload/start";
  static final String MOD_UPLOAD_COMPLETE_API_POST = "/mods/upload/complete";

  private final FafApiAccessor fafApiAccessor;
  private final I18n i18n;
  private final DataPrefs dataPrefs;
  private final WebClient defaultWebClient;

  @Setter
  private Path modPath;

  @Autowired
  public ModUploadTask(FafApiAccessor fafApiAccessor, I18n i18n, DataPrefs dataPrefs, WebClient defaultWebClient) {
    super(Priority.HIGH);
    this.dataPrefs = dataPrefs;
    this.fafApiAccessor = fafApiAccessor;
    this.i18n = i18n;
    this.defaultWebClient = defaultWebClient;
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(modPath, "modPath must not be null");

    ResourceLocks.acquireUploadLock();
    Path cacheDirectory = dataPrefs.getCacheDirectory();
    Files.createDirectories(cacheDirectory);
    Path tmpFile = createTempFile(cacheDirectory, "mod", ".zip");

    try {
      log.debug("Zipping mod `{}` to `{}`", modPath, tmpFile);
      updateTitle(i18n.get("modVault.upload.compressing"));

      Locale locale = i18n.getUserSpecificLocale();
      ByteCountListener byteListener = (written, total) -> {
        updateMessage(i18n.get("bytesProgress", formatSize(written, locale), formatSize(total, locale)));
        updateProgress(written, total);
      };

      try (OutputStream outputStream = newOutputStream(tmpFile)) {
        Zipper.of(modPath)
            .to(outputStream)
            .listener(byteListener)
            .zip();
      }

      log.debug("Starting upload sequence. Uploading mod `{}` as `{}`", modPath, tmpFile);
      updateTitle(i18n.get("modVault.upload.uploading"));

      return fafApiAccessor.getApiObject(MOD_UPLOAD_START_API_GET, UploadUrlResponse.class)
                           .flatMap(response -> uploadModToS3(response, tmpFile))
                           .flatMap(this::completeUpload)
                           .block();

    } finally {
      Files.delete(tmpFile);
      ResourceLocks.freeUploadLock();
    }
  }

  private Mono<Void> completeUpload(UUID requestId) {
    ModUploadMetadata metadata = new ModUploadMetadata(requestId, null, null);
    return fafApiAccessor.postJson(MOD_UPLOAD_COMPLETE_API_POST, metadata)
                         .doOnSuccess(response -> log.debug("Mod upload complete for requestId=[{}]", requestId));
  }

  private Mono<UUID> uploadModToS3(UploadUrlResponse response, Path filePath) {
    final URI signedUrl = response.uploadUrl();
    final UUID requestId = response.requestId();
    final FileSystemResource resource = new FileSystemResource(filePath);

    log.debug("Uploading mod to S3: requestId=[{}], zip filePath=[{}]", filePath, requestId);

    return defaultWebClient.put()
                           .uri(signedUrl)
                           .accept(MediaType.APPLICATION_JSON)
                           .contentType(MediaType.valueOf("application/zip"))
                           .body(BodyInserters.fromResource(resource))
                           .retrieve()
                           .onStatus(HttpStatusCode::isError, errResponse -> errResponse.bodyToMono(String.class)
                                                                                        .doOnNext(json -> log.warn(
                                                                                            "S3 Mod Upload failed. requestId=[{}], statusCode=[{}], \n response=[{}]",
                                                                                            requestId,
                                                                                            errResponse.statusCode()
                                                                                                       .value(), json))
                                                                                        .then(Mono.error(
                                                                                            new IllegalStateException(
                                                                                                "S3 Mod Upload failed. Request Id=[%s], Status Code=[%d]".formatted(
                                                                                                    requestId,
                                                                                                    errResponse.statusCode()
                                                                                                               .value())))))
                           .bodyToMono(Void.class)
                           .doOnSuccess(r -> log.debug("Successfully uploaded mod to S3: requestId=[{}]", requestId))
                           .thenReturn(requestId);
  }
}
