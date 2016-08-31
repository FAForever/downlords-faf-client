package com.faforever.client.remote.domain;

import java.net.URL;
import java.util.Objects;

public class SelectAvatarMessage extends ClientMessage {

  private final String action;

  private String avatar;

  public SelectAvatarMessage(URL url) {
    super(ClientMessageType.AVATAR);
    avatar = Objects.toString(url, "");
    this.action = "select";
  }
}
