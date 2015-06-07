package com.faforever.client.legacy.map;

import com.faforever.client.game.MapInfoBean;

import java.io.IOException;
import java.util.List;

public interface MapVaultParser {

  /**
   * Accesses the map vault over its HTML/AJAX thingy and parses the result. Since this isn't an API it's in no way
   * stable and this code breaks whenever the returned HTML changes.
   */
  List<MapInfoBean> parseMapVault() throws IOException;
}
