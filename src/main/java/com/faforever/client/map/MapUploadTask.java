package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.io.Zipper;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import static com.faforever.client.io.Bytes.formatSize;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapUploadTask extends CompletableTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  PreferencesService preferencesService;
  @Inject
  FafApiAccessor fafApiAccessor;
  @Inject
  I18n i18n;

  private Path mapPath;
  private Boolean isRanked;

  public MapUploadTask() {
    super(Priority.HIGH);
  }

  @PostConstruct
  void postConstruct() {
    updateTitle(i18n.get("mapVault.upload.uploading"));
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(mapPath, "mapPath must not be null");
    Validator.notNull(isRanked, "isRanked must not be null");

    ResourceLocks.acquireUploadLock();
    Path cacheDirectory = preferencesService.getCacheDirectory();
    Files.createDirectories(cacheDirectory);
    Path tmpFile = createTempFile(cacheDirectory, "map", ".zip");

    try {
      logger.debug("Zipping map {} to {}", mapPath, tmpFile);
      updateTitle(i18n.get("mapVault.upload.compressing"));

      Locale locale = i18n.getLocale();
      ByteCountListener byteListener = (written, total) -> {
        updateMessage(i18n.get("bytesProgress", formatSize(written, locale), formatSize(total, locale)));
        updateProgress(written, total);
      };

      try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(newOutputStream(tmpFile)))) {
        Zipper.of(mapPath)
            .to(zipOutputStream)
            .listener(byteListener)
            .zip();
      }

      logger.debug("Uploading map {} as {}", mapPath, tmpFile);
      updateTitle(i18n.get("mapVault.upload.uploading"));

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

  public void setRanked(boolean ranked) {
    isRanked = ranked;
  }
}
