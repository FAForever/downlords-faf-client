package com.faforever.client.mod;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.task.PrioritizedCompletableTask;
import com.faforever.client.util.Validator;
import com.faforever.commons.io.ByteCountListener;
import com.faforever.commons.io.Zipper;
import lombok.extern.slf4j.Slf4j;
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
public class ModUploadTask extends PrioritizedCompletableTask<Void> {

  private final FafApiAccessor fafApiAccessor;
  private final I18n i18n;
  private final DataPrefs dataPrefs;

  private Path modPath;

  @Autowired
  public ModUploadTask(FafApiAccessor fafApiAccessor, I18n i18n, DataPrefs dataPrefs) {
    super(Priority.HIGH);
    this.dataPrefs = dataPrefs;
    this.fafApiAccessor = fafApiAccessor;
    this.i18n = i18n;
  }

  @Override
  protected Void call() throws Exception {
    Validator.notNull(modPath, "modPath must not be null");

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

      log.debug("Uploading mod `{}` as `{}`", modPath, tmpFile);
      updateTitle(i18n.get("modVault.upload.uploading"));

      return fafApiAccessor.uploadFile("/mods/upload", tmpFile, byteListener, Map.of()).block();
    } finally {
      Files.delete(tmpFile);
    }
  }

  public void setModPath(Path modPath) {
    this.modPath = modPath;
  }
}
