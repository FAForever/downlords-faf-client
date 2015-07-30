package com.faforever.client.legacy.domain;

public class Notice extends ServerObject {

  public String text;
  public String style;

  @Override
  public String toString() {
    return "NoticeInfo{" +
        "style='" + style + '\'' +
        ", text='" + text + '\'' +
        '}';
  }
}
