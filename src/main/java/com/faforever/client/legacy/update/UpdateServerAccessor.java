package com.faforever.client.legacy.update;

import com.faforever.client.game.ModInfoBean;

import java.io.IOException;

public interface UpdateServerAccessor {

  /**
   * Requests a list of files going into the "bin" directory for a given mod.
   *
   * @param modInfoBean the mod to get the files for
   */
  void requestBinFilesToUpdate(ModInfoBean modInfoBean) throws IOException;

  /**
   * Requests a list of files going into the "gamedata" directory for a given mod.
   *
   * @param modInfoBean the mod to get the files for
   */
  void requestGameDataFilesToUpdate(ModInfoBean modInfoBean);
}
