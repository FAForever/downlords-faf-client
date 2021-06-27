package com.faforever.client.remote.domain.inbound.faf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class MatchFoundMessage extends FafInboundMessage {
  public static final String COMMAND = "match_found";

  @JsonProperty("queue_name")
  String queueName;
}
