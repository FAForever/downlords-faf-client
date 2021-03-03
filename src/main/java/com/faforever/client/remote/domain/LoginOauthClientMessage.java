package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class LoginOauthClientMessage extends ClientMessage {

  private String token;
  private long session;
  private String uniqueId;

  public LoginOauthClientMessage(String token, long session, String uniqueId) {
    super(ClientMessageType.OAUTH_LOGIN);
    this.setToken(token);
    this.setSession(session);
    this.setUniqueId(uniqueId);
  }

  @Override
  public Collection<String> getStringsToMask() {
    return List.of(getToken(), getUniqueId());
  }

}
