package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.io.Zipper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Validator;

import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

public class ModUploadTask extends CompletableTask<Void> {

  @Resource
  PreferencesService preferencesService;
  @Resource
  FafApiAccessor fafApiAccessor;

  private Path modPath;
  private Consumer<Float> progressListener;

  public ModUploadTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(modPath, "modPath must not be null");
    Validator.notNull(progressListener, "progressListener must not be null");

    ResourceLocks.acquireUploadLock();
    Path cacheDirectory = preferencesService.getCacheDirectory();
    Files.createDirectories(cacheDirectory);
    Path tmpFile = createTempFile(cacheDirectory, "mod", ".zip");
    try {
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(newOutputStream(tmpFile)))) {
        Zipper.of(modPath)
            .to(zipOutputStream)
            .listener((written, total) -> progressListener.accept((float) (written / total)))
            .zip();
      }

      fafApiAccessor.uploadMod(tmpFile);
      Files.delete(tmpFile);
      return null;
    } finally {
      ResourceLocks.freeUploadLock();
    }
  }

  public void setModPath(Path modPath) {
    this.modPath = modPath;
  }

  public void setProgressListener(Consumer<Float> progressListener) {
    this.progressListener = progressListener;
  }
}
