package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.commons.api.dto.Faction;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;


@Value
@EqualsAndHashCode(callSuper = true)
public class UpdatePartyMessage extends FafInboundMessage {
  public static final String COMMAND = "update_party";

  Integer owner;
  List<PartyMember> members;

  @Value
  public static class PartyMember {
    Integer player;
    List<String> factions;

    // jackson deserializes factions based on name, faction ids are used here
    public List<Faction> getFactions() {
      return factions.stream().map(Faction::fromString).collect(Collectors.toList());
    }
  }
}
