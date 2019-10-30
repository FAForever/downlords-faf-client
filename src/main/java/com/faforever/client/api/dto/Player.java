package com.faforever.client.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("player")
public class Player {
  @Id
  private String id;
  private String login;
  private String userAgent;

  @Relationship("globalRating")
  private GlobalRating globalRating;

  @Relationship("ladder1v1Rating")
  private Ladder1v1Rating ladder1v1Rating;

  @Relationship("names")
  private List<NameRecord> names;

  @Relationship("lobbyGroup")
  private LobbyGroup lobbyGroup;
}
