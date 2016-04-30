package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.io.Zipper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.ResourceLocks;

import javax.annotation.Resource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

public class UploadModTask extends AbstractPrioritizedTask<Void> {

  @Resource
  PreferencesService preferencesService;
  @Resource
  FafApiAccessor fafApiAccessor;

  private Path modPath;
  private Consumer<Float> progressListener;

  public UploadModTask() {
    super(Priority.HIGH);
  }

  @Override
  protected Void call() throws Exception {
    ResourceLocks.acquireUploadLock();
    ResourceLocks.acquireDiskLock();
    try {
      noCatch(() -> {
        Path tmpFile = createTempFile(preferencesService.getCacheDirectory(), "mod", "zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(newOutputStream(tmpFile)))) {
          Zipper.of(modPath)
              .to(zipOutputStream)
              .listener((written, total) -> progressListener.accept((float) (written / total)))
              .zip();
        }

        fafApiAccessor.uploadMod(new BufferedInputStream(Files.newInputStream(tmpFile)));
        Files.delete(tmpFile);
      });
      return null;
    } finally {
      ResourceLocks.acquireDiskLock();
      ResourceLocks.acquireUploadLock();
    }
  }

  public void setModPath(Path modPath) {
    this.modPath = modPath;
  }

  public void setProgressListener(Consumer<Float> progressListener) {
    this.progressListener = progressListener;
  }
}
