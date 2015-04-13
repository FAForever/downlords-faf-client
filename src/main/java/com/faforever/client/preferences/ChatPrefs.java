package com.faforever.client.preferences;

public class ChatPrefs {

  private Double zoom;

  public ChatPrefs() {
    this.maxItems = 500;
  }

  private int maxItems;

  public int getMaxItems() {
    return maxItems;
  }

  public void setZoom(Double zoom) {
    this.zoom = zoom;
  }

  public Double getZoom() {
    return zoom;
  }
}
