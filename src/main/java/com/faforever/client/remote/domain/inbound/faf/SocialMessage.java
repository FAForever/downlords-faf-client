package com.faforever.client.remote.domain.inbound.faf;


import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Value
public class SocialMessage extends FafInboundMessage {
  public static final String COMMAND = "social";

  List<Integer> friends;
  List<Integer> foes;
  List<String> channels;
}
