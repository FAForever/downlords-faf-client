package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.reporting.ReportingService;

import java.util.Arrays;

public class ImmediateErrorNotification extends ImmediateNotification {

  public ImmediateErrorNotification(String title, String text, Throwable throwable, I18n i18n, ReportingService reportingService) {
    super(title, text, Severity.ERROR, throwable, Arrays.asList(
        new ReportAction(i18n, reportingService, throwable),
        new DismissAction(i18n)
    ));
  }
}
