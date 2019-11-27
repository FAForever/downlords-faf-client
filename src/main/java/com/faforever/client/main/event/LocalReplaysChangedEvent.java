package com.faforever.client.main.event;

import com.faforever.client.replay.Replay;
import lombok.Value;

import java.util.Collection;

@Value
public class LocalReplaysChangedEvent {
  private final Collection<Replay> newReplays;
  private final Collection<Replay> deletedReplays;
}

