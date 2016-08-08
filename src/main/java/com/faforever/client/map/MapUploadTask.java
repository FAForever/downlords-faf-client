package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.io.ByteCountListener;
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
import java.util.zip.ZipOutputStream;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

public class MapUploadTask extends AbstractPrioritizedTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  PreferencesService preferencesService;
  @Resource
  FafApiAccessor fafApiAccessor;

  private Path mapPath;
  private CompletableFuture<Void> future;
  private Boolean isRanked;
  private ByteCountListener byteListener;

  public MapUploadTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(mapPath, "mapPath must not be null");
    Validator.notNull(byteListener, "byteListener must not be null");
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
            .listener(byteListener)
            .zip();
      }

      logger.debug("Uploading map {}", mapPath, tmpFile);

      fafApiAccessor.uploadMap(tmpFile, isRanked, byteListener);
      return null;
    } finally {
      Files.delete(tmpFile);
      ResourceLocks.freeUploadLock();
    }
  }

  public void setMapPath(Path mapPath) {
    this.mapPath = mapPath;
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

  public void setByteListener(ByteCountListener byteListener) {
    this.byteListener = byteListener;
  }
}
