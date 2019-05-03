package com.faforever.client.discord;

import lombok.Data;

@Data
public class DiscordSpectateEvent {
  private final Integer replayId;
  private final Integer playerId;
}
