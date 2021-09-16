package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Validator;
import com.faforever.commons.io.ByteCountListener;
import com.faforever.commons.io.Zipper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static com.faforever.commons.io.Bytes.formatSize;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newOutputStream;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MapUploadTask extends CompletableTask<Void> implements InitializingBean {

  private final PreferencesService preferencesService;
  private final FafApiAccessor fafApiAccessor;
  private final I18n i18n;

  private Path mapPath;
  private Boolean isRanked;

  @Inject
  public MapUploadTask(PreferencesService preferencesService, FafApiAccessor fafApiAccessor, I18n i18n) {
    super(Priority.HIGH);
    this.preferencesService = preferencesService;
    this.fafApiAccessor = fafApiAccessor;
    this.i18n = i18n;
  }

  @Override
  public void afterPropertiesSet() {
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
      log.debug("Zipping map {} to {}", mapPath, tmpFile);
      updateTitle(i18n.get("mapVault.upload.compressing"));

      Locale locale = i18n.getUserSpecificLocale();
      ByteCountListener byteListener = (written, total) -> {
        updateMessage(i18n.get("bytesProgress", formatSize(written, locale), formatSize(total, locale)));
        updateProgress(written, total);
      };

      try (OutputStream outputStream = newOutputStream(tmpFile)) {
        Zipper.of(mapPath)
            .to(outputStream)
            .listener(byteListener)
            .zip();
      }

      log.debug("Uploading map {} as {}", mapPath, tmpFile);
      updateTitle(i18n.get("mapVault.upload.uploading"));

      return fafApiAccessor.uploadFile("/maps/upload", tmpFile, byteListener, Map.of("metadata", Map.of("getRanked", isRanked))).block();
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
