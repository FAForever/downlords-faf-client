package com.faforever.client.legacy.domain;

public class Notice extends ServerObject {

  public String text;
  /**
   * info, error, kick, kill
   */
  public String style;

  @Override
  public String toString() {
    return "NoticeInfo{" +
        "style='" + style + '\'' +
        ", text='" + text + '\'' +
        '}';
  }

  public boolean isError() {
    return "error".equals(style);
  }

}
