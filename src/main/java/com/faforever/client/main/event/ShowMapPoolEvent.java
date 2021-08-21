package com.faforever.client.main.event;

import com.faforever.client.domain.MatchmakerQueueBean;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ShowMapPoolEvent extends OpenMapVaultEvent {
  MatchmakerQueueBean queue;
}