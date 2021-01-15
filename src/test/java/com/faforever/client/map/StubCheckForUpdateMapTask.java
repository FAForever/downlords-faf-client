package com.faforever.client.map;

import com.faforever.client.i18n.I18n;

import java.util.Optional;

public class StubCheckForUpdateMapTask extends CheckForUpdateMapTask {

  private MapBean mapFromServer;

  public StubCheckForUpdateMapTask(MapService mapService, I18n i18n) {
    super(mapService, i18n);
  }

  public void setMapFromServer(MapBean mapFromServer) {
    this.mapFromServer = mapFromServer;
  }

  @Override
  protected Optional<MapBean> call() throws Exception {
    // if null then map from server has no new version
    return mapFromServer != null ? Optional.of(mapFromServer) : Optional.empty();
  }
}
