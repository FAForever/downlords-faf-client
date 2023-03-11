package com.faforever.client.main.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class ShowUserReplaysEvent extends OpenOnlineReplayVaultEvent {
  int playerId;
}
