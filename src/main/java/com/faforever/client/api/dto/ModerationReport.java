package com.faforever.client.api.dto;

import com.faforever.commons.api.dto.ModerationReportStatus;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("moderationReport")
public class ModerationReport {

  @Id
  private String id;
  private String reportDescription;
  private String gameIncidentTimecode;
  private ModerationReportStatus reportStatus;

  @Relationship("game")
  private Game game;

  @Relationship("reporter")
  private Player reporter;

  @Relationship("reportedUsers")
  private Set<Player> reportedUsers;
}
