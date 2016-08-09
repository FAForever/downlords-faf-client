package com.faforever.client.map.event;

import com.faforever.client.map.MapBean;

public class MapUploadedEvent {
  private MapBean mapInfo;

  public MapUploadedEvent(MapBean mapBean) {
    this.mapInfo = mapBean;
  }

  public MapBean getMapInfo() {
    return mapInfo;
  }
}
