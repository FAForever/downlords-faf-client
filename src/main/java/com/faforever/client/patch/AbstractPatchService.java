package com.faforever.client.patch;

import com.faforever.client.preferences.PreferencesService;

import javax.annotation.Resource;
import java.nio.file.Path;

public abstract class AbstractPatchService {

  @Resource
  PreferencesService preferencesService;

  /**
   * Since it's possible that the user has changed or never specified the game path, this method needs to be called
   * every time before any work is done.
   *
   * @return {@code true} if directories are set up correctly
   */
  protected boolean checkDirectories() {
    Path faDirectory = preferencesService.getPreferences().getForgedAlliance().getPath();
    return faDirectory != null;
  }
}
