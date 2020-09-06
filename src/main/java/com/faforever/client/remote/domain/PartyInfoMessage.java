package com.faforever.client.remote.domain;

import com.faforever.client.game.Faction;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class PartyInfoMessage extends FafServerMessage {

  private Integer owner;
  private List<PartyMember> members;

  public PartyInfoMessage() {
    super(FafServerMessageType.PARTY_UPDATE);
  }

  @Data
  public static class PartyMember {
    private Integer player;
    private Boolean ready;
    private List<Integer> factions;

    // gson deserializes factions based on name, faction ids are used here
    public List<Faction> getFactions() {
      return factions.stream().map(Faction::fromFaValue).collect(Collectors.toList());
    }
  }
}
