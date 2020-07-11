package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
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

    return getUpdateInfo(clientConfiguration);
  }

  abstract protected UpdateInfo getUpdateInfo(ClientConfiguration clientConfiguration);

  protected long calculateUpdateSize(@Nullable Configuration oldConfig, Configuration newConfig) {
    Map<String, FileMetadata> filesToDownload = newConfig.getFiles().stream()
        .collect(Collectors.toMap(this::key, Function.identity()));

    if (oldConfig != null) {
      oldConfig.getFiles().stream()
          .collect(Collectors.toMap(FileMetadata::getChecksum, Function.identity()))
          .forEach((checksum, fileMetadata) -> filesToDownload.remove(key(fileMetadata)));
    }

    return filesToDownload.values().stream()
        .filter(file -> file.getOs() == null || file.getOs() == OS.CURRENT)
        .mapToLong(FileMetadata::getSize)
        .sum();
  }

  @NotNull
  private String key(FileMetadata metadata) {
    return metadata.getPath().toString() + metadata.getChecksum();
  }

  protected Configuration readConfiguration(URL update4jConfigUrl) {
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
