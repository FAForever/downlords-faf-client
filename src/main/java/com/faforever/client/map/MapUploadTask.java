package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.task.PrioritizedCompletableTask;
import com.faforever.client.util.Validator;
import com.faforever.commons.io.ByteCountListener;
import com.faforever.commons.io.Zipper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
public class MapUploadTask extends PrioritizedCompletableTask<Void> implements InitializingBean {

  private final FafApiAccessor fafApiAccessor;
  private final I18n i18n;
  private final DataPrefs dataPrefs;

  private Path mapPath;
  private Boolean isRanked;

  @Autowired
  public MapUploadTask(FafApiAccessor fafApiAccessor, I18n i18n, DataPrefs dataPrefs) {
    super(Priority.HIGH);
    this.fafApiAccessor = fafApiAccessor;
    this.i18n = i18n;
    this.dataPrefs = dataPrefs;
  }

  @Override
  public void afterPropertiesSet() {
    updateTitle(i18n.get("mapVault.upload.uploading"));
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(mapPath, "mapPath must not be null");
    Validator.notNull(isRanked, "isRanked must not be null");

    Path cacheDirectory = dataPrefs.getCacheDirectory();
    Files.createDirectories(cacheDirectory);
    Path tmpFile = createTempFile(cacheDirectory, "map", ".zip");

    try {
      log.info("Zipping map `{}` to `{}`", mapPath, tmpFile);
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

      log.info("Uploading map `{}` as `{}`", mapPath, tmpFile);
      updateTitle(i18n.get("mapVault.upload.uploading"));

      return fafApiAccessor.uploadFile("/maps/upload", tmpFile, byteListener, Map.of("metadata", Map.of("isRanked", isRanked))).block();
    } finally {
      Files.delete(tmpFile);
    }
  }

  public void setMapPath(Path mapPath) {
    this.mapPath = mapPath;
  }

  public void setRanked(boolean ranked) {
    isRanked = ranked;
  }
}
