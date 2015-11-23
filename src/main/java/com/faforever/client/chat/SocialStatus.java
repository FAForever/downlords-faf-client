package com.faforever.client.chat;

public enum SocialStatus {
  FRIEND("friend"),
  FOE("foe"),
  OTHER(""),
  SELF("self");

  private String cssClass;

  SocialStatus(String cssClass) {
    this.cssClass = cssClass;
  }

  public String getCssClass() {
    return cssClass;
  }
}
