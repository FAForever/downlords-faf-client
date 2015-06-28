package com.faforever.client.preferences;

public class ChatPrefs {

  private Double zoom;
  private boolean learnedAutoComplete;

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

  public boolean hasLearnedAutoComplete() {
    return learnedAutoComplete;
  }

  public void setLearnedAutoComplete(boolean learnedAutoComplete) {
    this.learnedAutoComplete = learnedAutoComplete;
  }
}
