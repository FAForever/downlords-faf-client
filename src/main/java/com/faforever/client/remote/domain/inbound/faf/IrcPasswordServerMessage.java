package com.faforever.client.remote.domain.inbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class IrcPasswordServerMessage extends FafInboundMessage {
  public static final String COMMAND = "irc_password";

  String password;
  @Override
  public Collection<String> getStringsToMask() {
    return List.of(getPassword());
  }
}