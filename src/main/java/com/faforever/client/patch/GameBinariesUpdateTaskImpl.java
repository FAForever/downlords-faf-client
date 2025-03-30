package com.faforever.client.patch;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.error.GameUpdateException;
import com.faforever.client.i18n.I18n;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsWindows;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.client.util.Assert;
import com.faforever.client.util.Validator;
import com.faforever.commons.fa.ForgedAllianceExePatcher;
import com.faforever.commons.io.ByteCopier;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.setAttribute;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
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

  private final ForgedAllianceLaunchService forgedAllianceLaunchService;
  private final I18n i18n;
  private final PlatformService platformService;
  private final OperatingSystem operatingSystem;
  private final DataPrefs dataPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private final String fafExeUrl;

  private Integer version;
  private boolean forReplays;

  public GameBinariesUpdateTaskImpl(ForgedAllianceLaunchService forgedAllianceLaunchService, I18n i18n,
                                    PlatformService platformService, OperatingSystem operatingSystem,
                                    DataPrefs dataPrefs, ForgedAlliancePrefs forgedAlliancePrefs,
                                    ClientProperties clientProperties) {
    super(Priority.HIGH);

    this.forgedAllianceLaunchService = forgedAllianceLaunchService;
    this.i18n = i18n;
    this.platformService = platformService;
    this.operatingSystem = operatingSystem;
    this.dataPrefs = dataPrefs;
    this.forgedAlliancePrefs = forgedAlliancePrefs;

    this.fafExeUrl = clientProperties.getForgedAlliance().getExeUrl();
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("updater.binary.taskTitle"));
    Assert.checkNullIllegalState(version, "Field 'version' must not be null");
    log.info("Updating binaries to `{}`", version);

    Path exePath;
    if (forReplays) {
      exePath = forgedAllianceLaunchService.getReplayExecutablePath();
    } else {
      exePath = forgedAllianceLaunchService.getExecutablePath();
    }

    copyGameFilesToFafBinDirectory();
    downloadFafExeIfNecessary(exePath);
    if (ForgedAllianceExePatcher.readVersion(exePath) != version) {
      ForgedAllianceExePatcher.patchVersion(exePath, version);
    }
    log.trace("Binaries have been updated successfully");
    return null;
  }

  @VisibleForTesting
  void downloadFafExeIfNecessary(Path exePath) throws IOException {
    if (Files.exists(exePath)) {
      platformService.setUnixExecutableAndWritableBits(exePath);
      return;
    }
    ResourceLocks.acquireDownloadLock();
    try {
      log.debug("Downloading `{}` to `{}`", fafExeUrl, exePath);
      URLConnection urlConnection = new URL(fafExeUrl).openConnection();
      try (InputStream inputStream = urlConnection.getInputStream();
           OutputStream outputStream = Files.newOutputStream(exePath)) {
        ByteCopier.from(inputStream)
            .to(outputStream)
            .totalBytes(urlConnection.getContentLength())
            .listener(this::updateProgress)
            .copy();
      }
      platformService.setUnixExecutableAndWritableBits(exePath);
    } finally {
      ResourceLocks.freeDownloadLock();
    }
  }

  @VisibleForTesting
  void copyGameFilesToFafBinDirectory() {
    log.trace("Copying Forged Alliance binaries FAF folder");

    Path fafBinDirectory;
    if (forReplays) {
      fafBinDirectory = dataPrefs.getReplayBinDirectory();
    } else {
      fafBinDirectory = dataPrefs.getBinDirectory();
    }

    Path faBinPath = forgedAlliancePrefs.getInstallationPath().resolve("bin");

    try (Stream<Path> faBinPathStream = Files.list(faBinPath)) {
      createDirectories(fafBinDirectory);
      faBinPathStream
          .filter(path -> BINARIES_TO_COPY.contains(path.getFileName().toString()))
          .forEach(source -> {
            Path destination = fafBinDirectory.resolve(source.getFileName());

            try {
              log.debug("Copying file '{}' to '{}'", source, destination);
              if (!Files.exists(destination)) {
                copy(source, destination, REPLACE_EXISTING);
              }

              if (operatingSystem instanceof OsWindows) {
                setAttribute(destination, "dos:readonly", false);
              }
            } catch (IOException e) {
              throw new GameUpdateException("Unable to copy file " + source, e, "game.files.updateError", source);
            }
          });
    } catch (IOException e) {
      throw new GameUpdateException("Unable to copy necessary binary files ", e, "game.files.binaryCopyError", faBinPath, fafBinDirectory);
    }
  }

  @Override
  public void setVersion(ComparableVersion version) {
    String versionString = version.toString();
    if (!Validator.isInt(versionString)) {
      throw new IllegalArgumentException("Versions of featured preferences mods must be integers");
    }
    this.version = Integer.parseInt(versionString);
  }

  @Override
  public void setForReplays(boolean forReplays) {
    this.forReplays = forReplays;
  }
}
