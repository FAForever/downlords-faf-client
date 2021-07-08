package com.faforever.client.remote.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class IrcPasswordServerMessage extends FafServerMessage {
  private String password;

  public IrcPasswordServerMessage() {
    super(FafServerMessageType.IRC_PASSWORD);
  }

  @Override
  public Collection<String> getStringsToMask() {
    return List.of(getPassword());
  }
}