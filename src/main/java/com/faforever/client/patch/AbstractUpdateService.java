package com.faforever.client.patch;

import com.faforever.client.preferences.PreferencesService;

import javax.annotation.Resource;

public abstract class AbstractUpdateService {

  @Resource
  PreferencesService preferencesService;

  /**
   * Since it's possible that the user has changed or never specified the game path, this method needs to be called
   * every time before any work is done.
   *
   * @return {@code true} if directories are set up correctly
   */
  protected boolean checkDirectories() {
    return preferencesService.getPreferences().getForgedAlliance().getPath() != null;
  }
}
