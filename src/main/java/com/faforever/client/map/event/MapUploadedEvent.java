package com.faforever.client.map.event;

import com.faforever.client.domain.MapVersionBean;

public class MapUploadedEvent {
  private MapVersionBean mapInfo;

  public MapUploadedEvent(MapVersionBean mapBean) {
    this.mapInfo = mapBean;
  }

  public MapVersionBean getMapInfo() {
    return mapInfo;
  }
}
