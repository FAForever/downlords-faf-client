package com.faforever.client.main.event;

import com.faforever.client.teammatchmaking.MatchmakingQueue;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ShowMapPoolEvent extends OpenMapVaultEvent {
  MatchmakingQueue queue;
}