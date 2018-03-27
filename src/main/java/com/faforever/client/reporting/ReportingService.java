package com.faforever.client.reporting;

public interface ReportingService {

  void reportError(Throwable e);

  void setAutoReportingUser(String username, int userId);
}
