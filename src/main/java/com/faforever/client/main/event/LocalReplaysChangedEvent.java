package com.faforever.client.main.event;

import com.faforever.client.replay.Replay;
import lombok.Data;

import java.util.Collection;

@Data
public class LocalReplaysChangedEvent {
  private final Collection<Replay> newReplays;
  private final Collection<Replay> deletedReplays;
}

