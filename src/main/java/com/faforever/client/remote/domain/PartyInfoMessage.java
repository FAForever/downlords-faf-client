package com.faforever.client.remote.domain;

import lombok.Data;

import java.util.List;

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
    private List<Boolean> factions;
  }
}
