package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.io.Zipper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

public class UploadMapTask extends AbstractPrioritizedTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  PreferencesService preferencesService;
  @Resource
  FafApiAccessor fafApiAccessor;

  private Path mapPath;
  private Consumer<Float> progressListener;
  private CompletableFuture<Void> future;
  private Boolean isRanked;

  public UploadMapTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(mapPath, "mapPath must not be null");
    Validator.notNull(progressListener, "progressListener must not be null");
    Validator.notNull(isRanked, "isRanked must not be null");

    ResourceLocks.acquireUploadLock();
    Path cacheDirectory = preferencesService.getCacheDirectory();
    Files.createDirectories(cacheDirectory);
    Path tmpFile = createTempFile(cacheDirectory, "map", ".zip");

    try {
      logger.debug("Zipping map {} to {}", mapPath, tmpFile);

      try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(newOutputStream(tmpFile)))) {
        Zipper.of(mapPath)
            .to(zipOutputStream)
            .listener((written, total) -> progressListener.accept((float) (written / total)))
            .zip();
      }

      logger.debug("Uploading map {}", mapPath, tmpFile);

      String targetFileName = String.format("%s.zip", mapPath.getFileName().toString());
      fafApiAccessor.uploadMap(tmpFile, isRanked);
      return null;
    } finally {
      Files.delete(tmpFile);
      ResourceLocks.freeUploadLock();
    }
  }

  public void setMapPath(Path mapPath) {
    this.mapPath = mapPath;
  }

  public void setProgressListener(Consumer<Float> progressListener) {
    this.progressListener = progressListener;
  }

  public CompletableFuture<Void> getFuture() {
    return future;
  }

  public void setFuture(CompletableFuture<Void> future) {
    this.future = future;
  }

  public void setRanked(boolean ranked) {
    isRanked = ranked;
  }
}
