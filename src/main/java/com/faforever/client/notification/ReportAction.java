package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;

public class ReportAction extends Action {

  public ReportAction(I18n i18n, ReportingService reportingService, Throwable throwable) {
    super(i18n.get("report"), event -> reportingService.report(throwable));
  }
}
