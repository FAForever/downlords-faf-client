package com.faforever.client.main.event;

import com.faforever.client.domain.server.MatchmakerQueueInfo;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ShowMapPoolEvent extends OpenMapVaultEvent {
  MatchmakerQueueInfo queue;
}