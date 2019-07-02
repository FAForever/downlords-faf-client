package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import com.google.common.annotations.VisibleForTesting;
import lombok.SneakyThrows;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.regex.Pattern;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForUpdateTask extends CompletableTask<UpdateInfo> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern SEMVER_PATTERN = Pattern.compile("v\\d+(\\.\\d+)*[^.]*");

  private final I18n i18n;
  private final PreferencesService preferencesService;

  private ComparableVersion currentVersion;

  @VisibleForTesting
  FileSizeReader fileSizeReader = url -> url
      .openConnection()
      .getContentLength();

  public CheckForUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(Priority.LOW);
    this.i18n = i18n;
    this.preferencesService = preferencesService;
  }

  @Override
  protected UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    logger.info("Checking for client update");

    // .get() because this task runs asynchronously already
    ClientConfiguration clientConfiguration = preferencesService.getRemotePreferences().get();

    ReleaseInfo latestRelease = clientConfiguration.getLatestRelease();
    String version = latestRelease.getVersion();

    logger.info("Current version is {}, newest version is {}", currentVersion, version);

    if (!SEMVER_PATTERN.matcher(version).matches()) {
      return null;
    }

    // Strip the "v" prefix
    final ComparableVersion latestVersion = new ComparableVersion(version.substring(1));

    if (latestVersion.compareTo(currentVersion) < 1) {
      return null;
    }

    URL downloadUrl;
    if (com.sun.jna.Platform.isWindows()) {
      downloadUrl = latestRelease.getWindowsUrl();
    } else if (com.sun.jna.Platform.isLinux()) {
      downloadUrl = latestRelease.getLinuxUrl();
    } else if (com.sun.jna.Platform.isMac()) {
      downloadUrl = latestRelease.getMacUrl();
    } else {
      return null;
    }
    if (downloadUrl == null) {
      return null;
    }

    int fileSize = getFileSize(downloadUrl);

    return new UpdateInfo(
        latestVersion.getCanonical(),
        downloadUrl.getFile().substring(downloadUrl.getFile().lastIndexOf('/') + 1),
        downloadUrl,
        fileSize,
        latestRelease.getReleaseNotesUrl()
    );
  }

  @SneakyThrows
  private int getFileSize(URL downloadUrl) {
    return fileSizeReader.read(downloadUrl);
  }

  public void setCurrentVersion(ComparableVersion currentVersion) {
    this.currentVersion = currentVersion;
  }

  // TODO make this available as a bean and use it in MapService as well
  @VisibleForTesting
  interface FileSizeReader {
    Integer read(URL url) throws IOException;
  }
}
