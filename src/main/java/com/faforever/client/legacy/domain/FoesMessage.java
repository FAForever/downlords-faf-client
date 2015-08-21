package com.faforever.client.legacy.domain;

import java.util.Collection;

public class FoesMessage extends ClientMessage {

  private Collection<String> foes;

  public FoesMessage(Collection<String> foes) {
    this.setCommand(ClientMessageType.SOCIAL);
    this.setFoes(foes);
  }

  public Collection<String> getFoes() {
    return foes;
  }

  public void setFoes(Collection<String> foes) {
    this.foes = foes;
  }
}
