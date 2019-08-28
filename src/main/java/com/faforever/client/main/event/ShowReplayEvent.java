package com.faforever.client.main.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShowReplayEvent extends OpenOnlineReplayVaultEvent {
  private final int replayId;
}
