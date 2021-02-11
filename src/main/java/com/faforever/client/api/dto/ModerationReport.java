package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Set;

@Type("moderationReport")
@Data
public class ModerationReport {
  @Id
  private String id;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
  private String reportDescription;
  private ModerationReportStatus reportStatus;
  private String gameIncidentTimecode;
  private String moderatorNotice;
  private String moderatorPrivateNote;

  @Relationship("bans")
  private Set<BanInfo> bans;
  @Relationship("reporter")
  private Player reporter;
  @Relationship("game")
  private Game game;
  @Relationship("lastModerator")
  private Player lastModerator;
  @Relationship("reportedUsers")
  private Set<Player> reportedUsers;
}
