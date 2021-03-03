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
    return switch (style) {
      case "error" -> Severity.ERROR;
      case "warning" -> Severity.WARN;
      case "info" -> Severity.INFO;
      default -> Severity.INFO;
    };
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

  public String getStyle() {
    return style;
  }
}
