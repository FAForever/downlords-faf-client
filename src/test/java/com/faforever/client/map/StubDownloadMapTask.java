package com.faforever.client.map;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class StubDownloadMapTask extends DownloadMapTask {

  private final Path customMapsDirectory;
  public MapVersion mapToDownload;

  public StubDownloadMapTask(ForgedAlliancePrefs forgedAlliancePrefs, I18n i18n, Path customMapsDirectory) {
    super(i18n, forgedAlliancePrefs);
    this.customMapsDirectory = customMapsDirectory;
  }

  public void setMapToDownload(MapVersion map) {
    this.mapToDownload = map;
  }

  @Override
  protected Void call() throws Exception {
    imitateMapDownload();
    return null;
  }

  private void imitateMapDownload() throws Exception {
    String folder = mapToDownload.folderName();
      FileSystemUtils.copyRecursively(
          Path.of(getClass().getResource("/maps/" + folder).toURI()),
          Files.createDirectories(customMapsDirectory.resolve(folder))
      );
  }
}
