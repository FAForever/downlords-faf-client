package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("ladder1v1LeaderboardEntry")
public class Ladder1v1LeaderboardEntry {
  @Id
  private String id;
  private int rank;
  private String name;
  private Double mean;
  private Double deviation;
  private Integer numGames;
  private Integer wonGames;
  private Boolean isActive;
  private Double rating;
}
