package com.faforever.client.api.dto;

import com.faforever.client.game.Faction;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("gamePlayerStats")
public class GamePlayerStats {
  @Id
  private String id;
  private boolean ai;
  private Faction faction;
  private byte color;
  private byte team;
  private byte startSpot;
  @Deprecated
  private Float beforeMean;
  @Deprecated
  private Float beforeDeviation;
  @Deprecated
  private Float afterMean;
  @Deprecated
  private Float afterDeviation;
  private byte score;
  @Nullable
  private OffsetDateTime scoreTime;

  @Relationship("game")
  private Game replay;

  @Relationship("player")
  private Player player;

  @Relationship("ratingChanges")
  private List<LeaderboardRatingJournal> leaderboardRatingJournals;
}
