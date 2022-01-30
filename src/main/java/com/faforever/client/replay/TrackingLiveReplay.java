package com.faforever.client.replay;

import lombok.Data;

@Data
public class TrackingLiveReplay {

  private final Integer gameId;
  private final LiveReplayAction action;
}
