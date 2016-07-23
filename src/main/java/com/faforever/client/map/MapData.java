package com.faforever.client.map;

import org.luaj.vm2.LuaTable;

public class MapData {

  private byte[] ddsData;
  private LuaTable markers;
  private float width;
  private float height;

  public LuaTable getMarkers() {
    return markers;
  }

  public void setMarkers(LuaTable markers) {
    this.markers = markers;
  }

  public float getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public float getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public byte[] getDdsData() {
    return ddsData;
  }

  public void setDdsData(byte[] ddsData) {
    this.ddsData = ddsData;
  }
}
