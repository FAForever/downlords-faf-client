package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import com.google.common.annotations.VisibleForTesting;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckForUpdateTask extends CompletableTask<UpdateInfo> {

  private final I18n i18n;
  private final PreferencesService preferencesService;
  public CheckForUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(Priority.LOW);
    this.i18n = i18n;
    this.preferencesService = preferencesService;
  }

  @VisibleForTesting
  int getFileSize(URL url) throws IOException {
    URLConnection connection = url.openConnection();
    Assert.state(connection instanceof HttpURLConnection, "Unexpected connection type: " + connection.getClass());

    HttpURLConnection httpConnection = (HttpURLConnection) connection;
    httpConnection.setRequestMethod("HEAD");

    int fileSize = httpConnection.getContentLength();
    httpConnection.disconnect();

    return fileSize;
  }

  @Override
  protected UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    log.info("Checking for client update");

    // no async call because this task runs asynchronously already
    ClientConfiguration clientConfiguration = preferencesService.getRemotePreferences();

    ReleaseInfo latestRelease = clientConfiguration.getLatestRelease();
    String version = latestRelease.getVersion();

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
        latestRelease.getReleaseNotesUrl(),
        false
    );
  }

  // TODO make this available as a bean and use it in MapService as well
  @VisibleForTesting
  interface FileSizeReader {
    Integer read(URL url) throws IOException;
  }
}
