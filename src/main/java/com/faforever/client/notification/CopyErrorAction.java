package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;

public class CopyErrorAction extends Action {

  public CopyErrorAction(I18n i18n, ReportingService reportingService, Throwable throwable) {
    super(i18n.get("copyError"), Type.OK_STAY, event -> reportingService.copyError(throwable));
  }
}
