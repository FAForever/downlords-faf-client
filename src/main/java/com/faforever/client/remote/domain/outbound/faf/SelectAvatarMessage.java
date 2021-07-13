package com.faforever.client.remote.domain.outbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.net.URL;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Value
public class SelectAvatarMessage extends AvatarMessage {

  String avatar;

  public SelectAvatarMessage(URL url) {
    super("select");
    avatar = Optional.ofNullable(url).map(URL::toString).orElse(null);
  }
}
