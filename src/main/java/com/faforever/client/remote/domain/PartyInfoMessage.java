package com.faforever.client.remote.domain;

import com.faforever.commons.api.dto.Faction;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class PartyInfoMessage extends FafServerMessage {

  private Integer owner;
  private List<PartyMember> members;

  public PartyInfoMessage() {
    super(FafServerMessageType.PARTY_UPDATE);
  }

  @Data
  public static class PartyMember {
    private Integer player;
    private List<String> factions;

    // gson deserializes factions based on name, faction ids are used here
    public List<Faction> getFactions() {
      return factions.stream().map(Faction::fromString).collect(Collectors.toList());
    }
  }
}
