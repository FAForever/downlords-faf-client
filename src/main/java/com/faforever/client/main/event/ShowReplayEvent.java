package com.faforever.client.main.event;

import com.faforever.client.replay.Replay;
import lombok.Data;

@Data
public class ShowReplayEvent extends OpenOnlineReplayVaultEvent {
  private final Replay replay;
}
