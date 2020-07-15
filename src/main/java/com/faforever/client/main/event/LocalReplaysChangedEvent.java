package com.faforever.client.main.event;

import com.faforever.client.replay.Replay;
import lombok.Value;

import java.util.Collection;

@Value
public class LocalReplaysChangedEvent {
  int page;
  int totalPages;
  Collection<Replay> replays;
}

