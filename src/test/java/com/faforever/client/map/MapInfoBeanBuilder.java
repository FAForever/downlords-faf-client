package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;

public class MapInfoBeanBuilder {

  private final MapInfoBean mapInfoBean;

  private MapInfoBeanBuilder() {
    mapInfoBean = new MapInfoBean();
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
