package com.faforever.client.remote.domain.outbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class LoginOauthClientMessage extends FafOutboundMessage {
  public static final String COMMAND = "auth";

  String token;
  long session;
  String uniqueId;

  @Override
  public Collection<String> getStringsToMask() {
    return List.of(getToken(), getUniqueId());
  }

}
