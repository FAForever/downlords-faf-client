package com.faforever.client.remote.domain.outbound.faf;

import com.faforever.client.game.GameVisibility;
import com.faforever.client.remote.domain.GameAccess;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Data sent from the client to the FAF server to tell it about a preferences to be hosted.
 */

@EqualsAndHashCode(callSuper = true)
@Value
public class HostGameMessage extends FafOutboundMessage {
  public static final String COMMAND = "game_host";

  GameAccess access;
  String mapname;
  String title;
  boolean[] options;
  String mod;
  String password;
  Integer version;
  GameVisibility visibility;
  Integer ratingMin;
  Integer ratingMax;
  Boolean enforceRatingRange;
}