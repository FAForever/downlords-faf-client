package com.faforever.client.api.dto;

public enum ModerationReportStatus {
  AWAITING("report.awaiting"),
  PROCESSING("report.processing"),
  COMPLETED("report.completed"),
  DISCARDED("report.discarded");

  private final String i18nKey;

  ModerationReportStatus(String i18nKey) {
    this.i18nKey = i18nKey;
  }

  public String getI18nKey() {
    return i18nKey;
  }
}
