package com.faforever.client.api.dto;

import com.faforever.client.game.Faction;
import com.faforever.client.replay.Replay;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("game_player_stats")
public class GamePlayerStats {
  @Id
  private String id;
  private Player player;
  private boolean ai;
  private Faction faction;
  private byte color;
  private byte team;
  private byte startSpot;
  private Float beforeMean;
  private Float beforeDeviation;
  private Float afterMean;
  private Float afterDeviation;
  private byte score;
  private Instant scoreTime;
  private Replay replay;
}
