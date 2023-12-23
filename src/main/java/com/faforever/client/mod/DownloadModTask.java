package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.PrioritizedCompletableTask;
import com.faforever.commons.io.ByteCopier;
import com.faforever.commons.io.Unzipper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.faforever.client.task.PrioritizedCompletableTask.Priority.HIGH;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class DownloadModTask extends PrioritizedCompletableTask<Void> {

  private final I18n i18n;
  private final DataPrefs dataPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private URL url;

  @Autowired
  public DownloadModTask(I18n i18n, DataPrefs dataPrefs, ForgedAlliancePrefs forgedAlliancePrefs) {
    super(HIGH);

    this.i18n = i18n;
    this.dataPrefs = dataPrefs;
    this.forgedAlliancePrefs = forgedAlliancePrefs;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(url, "url has not been set");

    Path tempFile = Files.createTempFile(dataPrefs.getCacheDirectory(), "mod", null);

    log.info("Downloading mod from `{}` to `{}`", url, tempFile);
    updateTitle(i18n.get("downloadingModTask.downloading", url));

    Files.createDirectories(tempFile.getParent());

    URLConnection urlConnection = url.openConnection();
    int contentLength = urlConnection.getContentLength();

    try (InputStream inputStream = urlConnection.getInputStream();
         OutputStream outputStream = Files.newOutputStream(tempFile)) {

      ByteCopier.from(inputStream)
          .to(outputStream)
          .listener(this::updateProgress)
          .totalBytes(contentLength)
          .copy();

      extractMod(tempFile);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }
    return null;
  }

  private void extractMod(Path tempFile) throws IOException, ArchiveException {
    Path modsDirectory = forgedAlliancePrefs.getModsDirectory();

    updateTitle(i18n.get("downloadingModTask.unzipping", modsDirectory));

    deleteOldModIfExisting(tempFile, modsDirectory);

    log.info("Unzipping `{}` to `{}`", tempFile, modsDirectory);
    try (InputStream inputStream = Files.newInputStream(tempFile)) {
      Unzipper.from(inputStream)
          .to(modsDirectory)
          .zipBombByteCountThreshold(100_000_000)
          .listener(this::updateProgress)
          .totalBytes(Files.size(tempFile))
          .unzip();

    }
  }

  private void deleteOldModIfExisting(Path tempFile, Path modsDirectory) {
    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(tempFile))) {
      ZipEntry zipEntry = zipInputStream.getNextEntry();
      Path pathToEntry = Path.of(zipEntry.getName());
      Path topLevelDirectory = getTopLevelDirectory(pathToEntry);
      Path modDirectory = modsDirectory.resolve(topLevelDirectory);
      if (Files.isDirectory(modDirectory)) {
        log.info("Deleting old mod version in `{}`", modDirectory);
        FileSystemUtils.deleteRecursively(modDirectory);
      }
    } catch (Exception e) {
      log.warn("Could not delete directory of old mod", e);
    }
  }

  private Path getTopLevelDirectory(Path pathToEntry) {
    Path cutOffPath = pathToEntry;
    while (cutOffPath.getParent() != null) {
      cutOffPath = cutOffPath.getParent();
    }
    return cutOffPath;
  }

  public void setUrl(URL url) {
    this.url = url;
  }
}
