package com.faforever.client.teammatchmaking.event;

import com.faforever.client.player.Player;
import lombok.Value;

@Value
public class PartyOwnerChangedEvent {
  Player newOwner;
}
