package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.domain.MatchmakingState;
import lombok.EqualsAndHashCode;
import lombok.Value;


@Value
@EqualsAndHashCode(callSuper = true)
public class SearchInfoMessage extends FafInboundMessage {
  public static final String COMMAND = "search_info";

  String queueName;
  MatchmakingState state;
}
