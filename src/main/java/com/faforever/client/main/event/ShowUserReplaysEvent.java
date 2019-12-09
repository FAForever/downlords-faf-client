package com.faforever.client.main.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShowUserReplaysEvent extends OpenOnlineReplayVaultEvent {
  private final int playerId;
}
