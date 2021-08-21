package com.faforever.client.builders;

import com.faforever.client.domain.ModerationReportBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.commons.api.dto.ModerationReportStatus;
import javafx.collections.FXCollections;

import java.time.OffsetDateTime;
import java.util.Set;


public class ModerationReportBeanBuilder {
  public static ModerationReportBeanBuilder create() {
    return new ModerationReportBeanBuilder();
  }

  private final ModerationReportBean moderationReportBean = new ModerationReportBean();

  public ModerationReportBeanBuilder defaultValues() {
    id(0);
    reportDescription("Bad things");
    reportStatus(ModerationReportStatus.PROCESSING);
    game(ReplayBeanBuilder.create().defaultValues().get());
    gameIncidentTimeCode("A time");
    moderatorNotice("do better");
    lastModerator(PlayerBeanBuilder.create().defaultValues().username("mod").get());
    reporter(PlayerBeanBuilder.create().defaultValues().username("junit").get());
    reportedUsers(FXCollections.observableSet(Set.of(PlayerBeanBuilder.create().defaultValues().username("offender").get())));
    return this;
  }

  public ModerationReportBeanBuilder reportDescription(String reportDescription) {
    moderationReportBean.setReportDescription(reportDescription);
    return this;
  }

  public ModerationReportBeanBuilder reportStatus(ModerationReportStatus reportStatus) {
    moderationReportBean.setReportStatus(reportStatus);
    return this;
  }

  public ModerationReportBeanBuilder gameIncidentTimeCode(String gameIncidentTimeCode) {
    moderationReportBean.setGameIncidentTimeCode(gameIncidentTimeCode);
    return this;
  }

  public ModerationReportBeanBuilder moderatorNotice(String moderatorNotice) {
    moderationReportBean.setModeratorNotice(moderatorNotice);
    return this;
  }

  public ModerationReportBeanBuilder lastModerator(PlayerBean lastModerator) {
    moderationReportBean.setLastModerator(lastModerator);
    return this;
  }

  public ModerationReportBeanBuilder reporter(PlayerBean reporter) {
    moderationReportBean.setReporter(reporter);
    return this;
  }

  public ModerationReportBeanBuilder reportedUsers(Set<PlayerBean> reportedUsers) {
    moderationReportBean.setReportedUsers(reportedUsers);
    return this;
  }

  public ModerationReportBeanBuilder game(ReplayBean game) {
    moderationReportBean.setGame(game);
    return this;
  }

  public ModerationReportBeanBuilder id(Integer id) {
    moderationReportBean.setId(id);
    return this;
  }

  public ModerationReportBeanBuilder createTime(OffsetDateTime createTime) {
    moderationReportBean.setCreateTime(createTime);
    return this;
  }

  public ModerationReportBeanBuilder updateTime(OffsetDateTime updateTime) {
    moderationReportBean.setUpdateTime(updateTime);
    return this;
  }

  public ModerationReportBean get() {
    return moderationReportBean;
  }

}

