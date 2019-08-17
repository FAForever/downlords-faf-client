package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import com.google.common.annotations.VisibleForTesting;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForUpdateTask extends CompletableTask<UpdateInfo> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final I18n i18n;
  private final PreferencesService preferencesService;

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

    if (!Version.shouldUpdate(Version.getCurrentVersion(), version)) {
      return null;
    }

    URL downloadUrl;
    if (org.bridj.Platform.isWindows()) {
      downloadUrl = latestRelease.getWindowsUrl();
    } else if (org.bridj.Platform.isLinux()) {
      downloadUrl = latestRelease.getLinuxUrl();
    } else if (org.bridj.Platform.isMacOSX()) {
      downloadUrl = latestRelease.getMacUrl();
    } else {
      return null;
    }
    if (downloadUrl == null) {
      return null;
    }

    int fileSize = getFileSize(downloadUrl);

    return new UpdateInfo(
        version,
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

  // TODO make this available as a bean and use it in MapService as well
  @VisibleForTesting
  interface FileSizeReader {
    Integer read(URL url) throws IOException;
  }
}
