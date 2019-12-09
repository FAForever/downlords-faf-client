package com.faforever.client.main.event;

import com.faforever.client.replay.Replay;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

public class LocalReplaysChangedEvent extends ApplicationEvent {

  private final Collection<Replay> newReplays;
  private final Collection<Replay> deletedReplays;

  public LocalReplaysChangedEvent(Object source, Collection<Replay> newReplays, Collection<Replay> deletedReplays) {
    super(source);
    this.newReplays = newReplays;
    this.deletedReplays = deletedReplays;
  }

  public Collection<Replay> getNewReplays() {
    return newReplays;
  }

  public Collection<Replay> getDeletedReplays() {
    return deletedReplays;
  }
}

