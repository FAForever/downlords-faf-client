package com.faforever.client.main.event;

import com.faforever.client.replay.Replay;
import lombok.Value;

import java.util.Collection;

@Value
public class LocalReplaysChangedEvent {
  Collection<Replay> newReplays;
  Collection<Replay> deletedReplays;
}

