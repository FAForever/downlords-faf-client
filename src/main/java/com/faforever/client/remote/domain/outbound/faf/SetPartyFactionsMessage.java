package com.faforever.client.remote.domain.outbound.faf;

import com.faforever.commons.api.dto.Faction;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Value
public class SetPartyFactionsMessage extends FafOutboundMessage {
  public static final String COMMAND = "set_party_factions";

  List<Faction> factions;
}
