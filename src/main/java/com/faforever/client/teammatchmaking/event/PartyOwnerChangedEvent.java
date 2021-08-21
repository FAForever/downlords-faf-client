package com.faforever.client.teammatchmaking.event;

import com.faforever.client.domain.PlayerBean;
import lombok.Value;

@Value
public class PartyOwnerChangedEvent {
  PlayerBean newOwner;
}
