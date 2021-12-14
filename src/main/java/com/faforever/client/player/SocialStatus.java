package com.faforever.client.player;

public enum SocialStatus {
  FRIEND("friend"),
  FOE("foe"),
  OTHER("other"),
  SELF("self");

  private final String cssClass;

  SocialStatus(String cssClass) {
    this.cssClass = cssClass;
  }

  public String getCssClass() {
    return cssClass;
  }
}
