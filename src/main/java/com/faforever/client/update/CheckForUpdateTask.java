package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsPosix;
import com.faforever.client.os.OsUnknown;
import com.faforever.client.os.OsWindows;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import com.faforever.client.util.FileSizeReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForUpdateTask extends CompletableTask<UpdateInfo> {

  private final I18n i18n;
  private final PreferencesService preferencesService;
  private final FileSizeReader fileSizeReader;
  private final OperatingSystem operatingSystem;

  public CheckForUpdateTask(I18n i18n, PreferencesService preferencesService, FileSizeReader fileSizeReader, OperatingSystem operatingSystem) {
    super(Priority.LOW);
    this.i18n = i18n;
    this.preferencesService = preferencesService;
    this.fileSizeReader  = fileSizeReader;
    this.operatingSystem = operatingSystem;
  }

  @Override
  protected UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    log.info("Checking for client update");

    return preferencesService.getRemotePreferencesAsync().thenApply(clientConfiguration -> {
      ReleaseInfo latestRelease = clientConfiguration.getLatestRelease();
      String version = latestRelease.getVersion();

      URL downloadUrl = switch (operatingSystem) {
        case OsWindows osWindows -> latestRelease.getWindowsUrl();
        case OsPosix osPosix -> latestRelease.getLinuxUrl();
        case OsUnknown osUnknown -> null;
      };

      if (downloadUrl == null) {
        return null;
      }

      int fileSize = fileSizeReader.getFileSize(downloadUrl).join();

      return new UpdateInfo(
          version,
          downloadUrl.getFile().substring(downloadUrl.getFile().lastIndexOf('/') + 1),
          downloadUrl,
          fileSize,
          latestRelease.getReleaseNotesUrl(),
          false
      );
    }).join();
  }
}
