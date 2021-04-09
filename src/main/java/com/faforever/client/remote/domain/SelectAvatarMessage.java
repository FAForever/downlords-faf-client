package com.faforever.client.remote.domain;

import java.net.URL;
import java.util.Optional;

public class SelectAvatarMessage extends ClientMessage {

  private final String action;

  private final String avatar;

  public SelectAvatarMessage(URL url) {
    super(ClientMessageType.AVATAR);
    avatar = Optional.ofNullable(url).map(URL::toString).orElse(null);
    this.action = "select";
  }
}
