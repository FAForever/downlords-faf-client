package com.faforever.client.remote.domain;

import com.faforever.client.notification.Severity;

public class NoticeMessage extends FafServerMessage {

  private String text;
  private String style;

  public NoticeMessage() {
    super(FafServerMessageType.NOTICE);
  }

  public Severity getSeverity() {
    if (style == null) {
      return Severity.INFO;
    }
    switch (style) {
      case "error":
        return Severity.ERROR;
      case "warning":
        return Severity.WARN;
      case "info":
        return Severity.INFO;
      default:
        return Severity.INFO;
    }
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setStyle(String style) {
    this.style = style;
  }
}
