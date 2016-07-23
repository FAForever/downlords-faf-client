package com.faforever.client.map;

public class MapBuilder {

  private final MapBean mapBean;

  private MapBuilder() {
    mapBean = new MapBean();
  }

  public MapBuilder defaultValues() {
    return displayName("Map name")
        .technicalName("map_name.v001")
        .mapSize(new MapSize(10, 10));
  }

  public MapBuilder mapSize(MapSize mapSize) {
    mapBean.setSize(mapSize);
    return this;
  }

  public MapBuilder technicalName(String technicalName) {
    mapBean.setTechnicalName(technicalName);
    return this;
  }

  public MapBuilder displayName(String displayName) {
    mapBean.setDisplayName(displayName);
    return this;
  }

  public MapBean get() {
    return mapBean;
  }

  public static MapBuilder create() {
    return new MapBuilder();
  }
}
