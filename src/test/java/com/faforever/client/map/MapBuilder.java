package com.faforever.client.map;

import com.faforever.client.map.MapBean.Type;

public class MapBuilder {

  private final MapBean mapBean;

  private MapBuilder() {
    mapBean = new MapBean();
  }

  public static MapBuilder create() {
    return new MapBuilder();
  }

  public MapBuilder defaultValues() {
    return displayName("Map name")
        .folderName("map_name.v001")
        .type(Type.SKIRMISH)
        .mapSize(MapSize.valueOf(512, 512));
  }

  public MapBuilder mapSize(MapSize mapSize) {
    mapBean.setSize(mapSize);
    return this;
  }

  public MapBuilder folderName(String technicalName) {
    mapBean.setFolderName(technicalName);
    return this;
  }

  public MapBuilder displayName(String displayName) {
    mapBean.setDisplayName(displayName);
    return this;
  }

  public MapBuilder type(Type type) {
    mapBean.setType(type);
    return this;
  }

  public MapBean get() {
    return mapBean;
  }
}
