package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.domain.RatingRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Value
public class MatchmakerInfoMessage extends FafInboundMessage {
  public static final String COMMAND = "matchmaker_info";

  List<MatchmakerQueue> queues;

  @Value
  public static class MatchmakerQueue {

    String queueName;
    String queuePopTime;
    int teamSize;
    int numPlayers;

    // The boundaries indicate the ranges applicable for other searching players,
    // boundarys.size() therefore indicates the players currently in queue
    @JsonProperty("boundary_75s")
    List<RatingRange> boundary75s;
    @JsonProperty("boundary_80s")
    List<RatingRange> boundary80s;
  }
}
