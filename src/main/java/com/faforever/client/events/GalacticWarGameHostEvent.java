package com.faforever.client.events;

import com.faforever.client.remote.domain.GameLaunchMessage;
import lombok.Data;

@Data
public class GalacticWarGameHostEvent {
  private final GameLaunchMessage gameLaunchMessage;
}
