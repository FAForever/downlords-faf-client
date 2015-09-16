package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.util.ByteCopier;
import com.faforever.client.util.Unzipper;
import com.google.common.net.UrlEscapers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;

public class DownloadModTask extends PrioritizedTask<Void> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  Environment environment;

  @Autowired
  I18n i18n;

  private String modPath;

  @PostConstruct
  public void postConstruct() {
    setPriority(HIGH);
  }

  @Override
  protected Void call() throws Exception {
    String urlString = environment.getProperty("vault.modRoot") + UrlEscapers.urlPathSegmentEscaper().escape(modPath.replace("mods/", ""));

    Path tempFile = Files.createTempFile(preferencesService.getCacheDirectory(), "mod", null);

    logger.debug("Downloading mod {} to {}", urlString, tempFile);
    updateTitle(i18n.get("downloadingModTask.downloading", urlString));

    URL url = new URL(urlString);

    Files.createDirectories(tempFile.getParent());

    try (InputStream inputStream = url.openStream();
         OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .listener(this::updateProgress)
          .copy();

      extractMod(tempFile);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        logger.warn("Could not delete temporary file: " + tempFile.toAbsolutePath(), e);
      }
    }
    return null;
  }

  private void extractMod(Path tempFile) throws IOException {
    Path modsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();

    updateTitle(i18n.get("downloadingModTask.unzipping", modsDirectory));
    logger.debug("Unzipping {} to {}", tempFile, modsDirectory);

    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(tempFile))) {
      Unzipper.from(zipInputStream)
          .to(modsDirectory)
          .listener(this::updateProgress)
          .unzip();
    }
  }

  public void setModPath(String modPath) {
    this.modPath = modPath;
  }
}
