package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.game.MapSize;

public class MapInfoBeanBuilder {

  private final MapInfoBean mapInfoBean;

  private MapInfoBeanBuilder() {
    mapInfoBean = new MapInfoBean();
  }

  public MapInfoBeanBuilder defaultValues() {
    return displayName("Map name")
        .technicalName("map_name.v001")
        .mapSize(new MapSize(10, 10));
  }

  public MapInfoBeanBuilder mapSize(MapSize mapSize) {
    mapInfoBean.setSize(mapSize);
    return this;
  }

  public MapInfoBeanBuilder technicalName(String technicalName) {
    mapInfoBean.setTechnicalName(technicalName);
    return this;
  }

  public MapInfoBeanBuilder displayName(String displayName) {
    mapInfoBean.setDisplayName(displayName);
    return this;
  }

  public MapInfoBean get() {
    return mapInfoBean;
  }

  public static MapInfoBeanBuilder create() {
    return new MapInfoBeanBuilder();
  }
}
