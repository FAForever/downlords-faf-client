package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.notification.Severity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class NoticeMessage extends FafInboundMessage {
  public static final String COMMAND = "notice";

  String text;
  String style;

  @JsonIgnore
  public Severity getSeverity() {
    if (style == null) {
      return Severity.INFO;
    }
    return switch (style) {
      case "error" -> Severity.ERROR;
      case "warning" -> Severity.WARN;
      default -> Severity.INFO;
    };
  }
}
