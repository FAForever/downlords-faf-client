package com.faforever.client.remote.domain;

public class PartyKickedMessage extends FafServerMessage {

  public PartyKickedMessage() {
    super(FafServerMessageType.PARTY_KICKED);
  }
}
