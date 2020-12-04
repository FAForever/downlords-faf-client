package com.faforever.client.rankedmatch;

import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.RatingRange;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class MatchmakerInfoMessage extends FafServerMessage {

  @Data
  public static class MatchmakerQueue {

    private String queueName;
    private String queuePopTime;
    @SerializedName("team_size")
    private int teamSize;
    @SerializedName("num_players")
    private int numPlayers;

    // The boundaries indicate the ranges applicable for other searching players,
    // boundarys.size() therefore indicates the players currently in queue
    @SerializedName("boundary_75s")
    private List<RatingRange> boundary75s;
    @SerializedName("boundary_80s")
    private List<RatingRange> boundary80s;

    public MatchmakerQueue(String queueName, String queuePopTime, int teamSize, int numPlayers, List<RatingRange> boundary75s, List<RatingRange> boundary80s) {
      this.queueName = queueName;
      this.queuePopTime = queuePopTime;
      this.teamSize = teamSize;
      this.numPlayers = numPlayers;
      this.boundary75s = boundary75s;
      this.boundary80s = boundary80s;
    }

  }
  @Getter
  @Setter
  private List<MatchmakerQueue> queues;

  public MatchmakerInfoMessage() {
    super(FafServerMessageType.MATCHMAKER_INFO);
  }

}
