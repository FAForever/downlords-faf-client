package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Assert;
import com.faforever.client.util.Validator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import static com.faforever.client.preferences.PreferencesService.FORGED_ALLIANCE_EXE;
import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.setAttribute;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GameBinariesUpdateTaskImpl extends CompletableTask<Void> implements GameBinariesUpdateTask {

  @VisibleForTesting
  static final Collection<String> BINARIES_TO_COPY = Arrays.asList(
      "BsSndRpt.exe",
      "BugSplat.dll",
      "BugSplatRc.dll",
      "DbgHelp.dll",
      "GDFBinary.dll",
      "Microsoft.VC80.CRT.manifest",
      "SHSMP.DLL",
      "msvcm80.dll",
      "msvcp80.dll",
      "msvcr80.dll",
      "sx32w.dll",
      "wxmsw24u-vs80.dll",
      "zlibwapi.dll"
  );
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int[] VERSION_ADDRESSES = new int[]{0xd3d40, 0x47612d, 0x476666};

  private final I18n i18n;
  private final PreferencesService preferencesService;

  private final String fafExeUrl;

  private Integer version;

  @Inject
  public GameBinariesUpdateTaskImpl(I18n i18n, PreferencesService preferencesService, @Value("${fafExe.url}") String fafExeUrl) {
    super(Priority.HIGH);

    this.i18n = i18n;
    this.preferencesService = preferencesService;

    this.fafExeUrl = fafExeUrl;
  }

  @VisibleForTesting
  static void updateVersionInExe(Integer version, Path exePath) throws IOException {
    byte[] versionAsLittleEndianBytes = toLittleEndianByteArray(version);
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(exePath.toFile(), "rw")) {
      logger.debug("Updating version in {} to {}", exePath, version);
      for (int versionAddress : VERSION_ADDRESSES) {
        randomAccessFile.seek(versionAddress);
        randomAccessFile.write(versionAsLittleEndianBytes);
      }
    }
  }

  private static byte[] toLittleEndianByteArray(int i) {
    return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("updater.binary.taskTitle"));
    Assert.checkNullIllegalState(version, "Field 'version' must not be null");
    logger.info("Updating binaries to {}", version);

    Path exePath = preferencesService.getFafBinDirectory().resolve(FORGED_ALLIANCE_EXE);

    copyGameFilesToFafBinDirectory();
    downloadFafExeIfNecessary(exePath);
    updateVersionInExe(version, exePath);
    logger.debug("Binaries have been updated successfully");
    return null;
  }

  private void downloadFafExeIfNecessary(Path exePath) throws IOException {
    if (Files.exists(exePath)) {
      return;
    }
    try {
      ResourceLocks.acquireDownloadLock();
      logger.debug("Downloading {} to {}", fafExeUrl, exePath);
      URLConnection urlConnection = new URL(fafExeUrl).openConnection();
      try (InputStream inputStream = urlConnection.getInputStream();
           OutputStream outputStream = Files.newOutputStream(exePath)) {
        ByteCopier.from(inputStream)
            .to(outputStream)
            .totalBytes(urlConnection.getContentLength())
            .listener(this::updateProgress)
            .copy();
      }
    } finally {
      ResourceLocks.freeDownloadLock();
    }
  }

  @VisibleForTesting
  void copyGameFilesToFafBinDirectory() throws IOException {
    logger.debug("Copying Forged Alliance binaries FAF folder");

    Path fafBinDirectory = preferencesService.getFafBinDirectory();
    createDirectories(fafBinDirectory);

    Path faBinPath = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("bin");

    Files.list(faBinPath)
        .filter(path -> BINARIES_TO_COPY.contains(path.getFileName().toString()))
        .forEach(source -> {
          Path destination = fafBinDirectory.resolve(source.getFileName());

          logger.debug("Copying file '{}' to '{}'", source, destination);
          noCatch(() -> createDirectories(destination.getParent()));
          noCatch(() -> copy(source, destination, REPLACE_EXISTING));

          if (org.bridj.Platform.isWindows()) {
            noCatch(() -> setAttribute(destination, "dos:readonly", false));
          }
        });
  }

  @Override
  public void setVersion(ComparableVersion version) {
    String versionString = version.toString();
    if (!Validator.isInt(versionString)) {
      throw new IllegalArgumentException("Versions of featured preferences mods must be integers");
    }
    this.version = Integer.parseInt(versionString);
  }
}
