package com.faforever.client.teammatchmaking.event;

import com.faforever.client.domain.PlayerBean;

public record PartyOwnerChangedEvent(PlayerBean newOwner) {
}
