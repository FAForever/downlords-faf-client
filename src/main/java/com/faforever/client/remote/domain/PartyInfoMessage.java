package com.faforever.client.remote.domain;

import java.util.List;

public class PartyInfoMessage extends FafServerMessage {

  private Integer owner;
  private List<Integer> members;
  private List<Integer> members_ready;

  public PartyInfoMessage() {
    super(FafServerMessageType.PARTY_UPDATE);
  }

  public Integer getOwner() {
    return owner;
  }

  public void setOwner(Integer owner) {
    this.owner = owner;
  }

  public List<Integer> getMembers() {
    return members;
  }

  public void setMembers(List<Integer> members) {
    this.members = members;
  }

  public List<Integer> getMembers_ready() {
    return members_ready;
  }

  public void setMembers_ready(List<Integer> members_ready) {
    this.members_ready = members_ready;
  }
}
