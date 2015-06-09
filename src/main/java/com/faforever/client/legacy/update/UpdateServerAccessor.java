package com.faforever.client.legacy.update;

import java.io.IOException;

public interface UpdateServerAccessor {

  /**
   * Requests a list of files going into the "bin" directory for a given mod.
   *
   * @param modName the mod to get the files for
   */
  void requestBinFilesToUpdate(String modName) throws IOException;

  /**
   * Requests a list of files going into the "gamedata" directory for a given mod.
   *
   * @param modName the mod to get the files for
   */
  void requestGameDataFilesToUpdate(String modName);

  void connect();
}
