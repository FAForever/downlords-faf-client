package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Paths;

public class StubDownloadMapTask extends DownloadMapTask {

  private final TemporaryFolder customMapsDirectory;
  public MapBean mapToDownload;

  public StubDownloadMapTask(PreferencesService preferencesService, I18n i18n, TemporaryFolder customMapsDirectory) {
    super(preferencesService, i18n);
    this.customMapsDirectory = customMapsDirectory;
  }

  public void setMapToDownload(MapBean map) {
    this.mapToDownload = map;
  }

  @Override
  protected Void call() throws Exception {
    imitateMapDownload();
    return null;
  }

  private void imitateMapDownload() throws Exception {
    String folder = mapToDownload.getFolderName();
      FileSystemUtils.copyRecursively(
          Paths.get(getClass().getResource("/maps/" + folder).toURI()),
          customMapsDirectory.newFolder(folder).toPath()
      );
  }
}
