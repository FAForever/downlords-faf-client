package com.faforever.client.teammatchmaking.event;

import com.faforever.client.domain.server.PlayerInfo;

public record PartyOwnerChangedEvent(PlayerInfo newOwner) {
}
