package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.OS;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public abstract class AbstractCheckForUpdateTask extends CompletableTask<UpdateInfo> {

  private final I18n i18n;
  private final PreferencesService preferencesService;

  public AbstractCheckForUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(Priority.LOW);
    this.i18n = i18n;
    this.preferencesService = preferencesService;
  }

  @Override
  protected final UpdateInfo call() throws Exception {
    updateTitle(i18n.get("clientUpdateCheckTask.title"));
    log().info("Checking for client update");

    ClientConfiguration clientConfiguration = preferencesService.getRemotePreferences();

    URL update4jConfigUrl = getUpdate4jConfigUrl(clientConfiguration);
    if (update4jConfigUrl == null) {
      log().warn("No update4jConfigUrl provided");
      return null;
    }

    Configuration newConfig = readConfiguration(update4jConfigUrl);
    if (newConfig == null) {
      return null;
    }

    URL currentVersionConfigUrl = getClass().getResource("/update4j/update4j.xml");
    Configuration oldConfig = readConfiguration(currentVersionConfigUrl);

    long size = calculateUpdateSize(oldConfig, newConfig);

    ReleaseInfo latestRelease = clientConfiguration.getLatestRelease();
    String version = latestRelease.getVersion();
    URL releaseNotesUrl = latestRelease.getReleaseNotesUrl();

    return new UpdateInfo(version, newConfig, size, releaseNotesUrl, false);
  }

  @Nullable
  protected URL getUpdate4jConfigUrl(ClientConfiguration clientConfiguration) {
    return clientConfiguration.getLatestRelease().getUpdate4jConfigUrl();
  }

  private long calculateUpdateSize(@Nullable Configuration oldConfig, Configuration newConfig) {
    Map<Long, FileMetadata> filesToDownload = newConfig.getFiles().stream()
        .collect(Collectors.toMap(FileMetadata::getChecksum, Function.identity()));

    if (oldConfig != null) {
      oldConfig.getFiles().stream()
          .collect(Collectors.toMap(FileMetadata::getChecksum, Function.identity()))
          .forEach((checksum, fileMetadata) -> filesToDownload.remove(checksum));
    }

    return filesToDownload.values().stream()
        .filter(file -> file.getOs() == null || file.getOs() == OS.CURRENT)
        .mapToLong(FileMetadata::getSize)
        .sum();
  }

  @Nullable
  private Configuration readConfiguration(URL update4jConfigUrl) {
    Configuration newConfig;
    try (Reader reader = new InputStreamReader(update4jConfigUrl.openStream())) {
      newConfig = Configuration.read(reader);
    } catch (Exception e) {
      log().warn("Error while reading config from {}", update4jConfigUrl, e);
      return null;
    }
    return newConfig;
  }

  protected abstract Logger log();
}
