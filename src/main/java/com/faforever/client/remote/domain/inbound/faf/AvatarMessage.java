package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.domain.Avatar;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Value
public class AvatarMessage extends FafInboundMessage {
  public static final String COMMAND = "avatar";

  @JsonProperty("avatarlist")
  List<Avatar> avatarList;
}
