package com.faforever.client.reporting;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.commons.api.dto.ModerationReportStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.time.LocalDateTime;
import java.util.Set;

public class ModerationReportBuilder {
  private final ModerationReport moderationReport = new ModerationReport();

  public static ModerationReportBuilder create() {
    return new ModerationReportBuilder();
  }

  public ModerationReportBuilder defaultValues() {
    reportId(0);
    reportDescription("Bad things");
    reportStatus(ModerationReportStatus.PROCESSING);
    gameId(0);
    gameIncidentTimeCode("A time");
    moderatorNotice("do better");
    lastModerator(PlayerBuilder.create("mod").defaultValues().get());
    reporter(PlayerBuilder.create("junit").defaultValues().get());
    reportedUsers(FXCollections.observableSet(Set.of(PlayerBuilder.create("offender").defaultValues().get())));
    createTime(LocalDateTime.MIN);
    updateTime(LocalDateTime.MAX);
    return this;
  }

  public ModerationReportBuilder reportId(int reportId) {
    moderationReport.setReportId(reportId);
    return this;
  }

  public ModerationReportBuilder reportDescription(String reportDescription) {
    moderationReport.setReportDescription(reportDescription);
    return this;
  }

  public ModerationReportBuilder reportStatus(ModerationReportStatus reportStatus) {
    moderationReport.setReportStatus(reportStatus);
    return this;
  }

  public ModerationReportBuilder gameIncidentTimeCode(String gameIncidentTimeCode) {
    moderationReport.setGameIncidentTimeCode(gameIncidentTimeCode);
    return this;
  }

  public ModerationReportBuilder moderatorNotice(String moderatorNotice) {
    moderationReport.setModeratorNotice(moderatorNotice);
    return this;
  }

  public ModerationReportBuilder lastModerator(Player lastModerator) {
    moderationReport.setLastModerator(lastModerator);
    return this;
  }

  public ModerationReportBuilder reporter(Player reporter) {
    moderationReport.setReporter(reporter);
    return this;
  }

  public ModerationReportBuilder reportedUsers(ObservableSet reportedUsers) {
    moderationReport.setReportedUsers(reportedUsers);
    return this;
  }

  public ModerationReportBuilder createTime(LocalDateTime createTime) {
    moderationReport.setCreateTime(createTime);
    return this;
  }

  public ModerationReportBuilder updateTime(LocalDateTime updateTime) {
    moderationReport.setUpdateTime(updateTime);
    return this;
  }

  public ModerationReportBuilder gameId(Integer gameId) {
    moderationReport.setGameId(gameId);
    return this;
  }

  public ModerationReport get() {
    return moderationReport;
  }

}
