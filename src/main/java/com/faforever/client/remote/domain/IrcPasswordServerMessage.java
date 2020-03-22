package com.faforever.client.remote.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IrcPasswordServerMessage extends FafServerMessage {
  private String password;

  public IrcPasswordServerMessage() {
    super(FafServerMessageType.IRC_PASSWORD);
  }
}
