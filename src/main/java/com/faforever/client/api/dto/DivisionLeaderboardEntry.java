package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("divisionLeaderboardEntry")
public class DivisionLeaderboardEntry {
  @Id
  private String id;
  private int rank;
  private String name;
  private Integer score;
  private Integer numGames;
  private Integer wonGames;
  private Boolean isActive;
  private Integer majorDivisionIndex;
  private Integer subDivisionIndex;
}
