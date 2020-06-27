package com.faforever.client.rankedmatch;

import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.RatingRange;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MatchmakerInfoMessage extends FafServerMessage {

  private String action; // TODO: doesn't exist anymore

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

    public String getQueueName() {
      return queueName;
    }

    public void setQueueName(String queueName) {
      this.queueName = queueName;
    }

    public String getQueuePopTime() {
      return queuePopTime;
    }

    public void setQueuePopTime(String queuePopTime) {
      this.queuePopTime = queuePopTime;
    }

    public List<RatingRange> getBoundary75s() {
      return boundary75s;
    }

    public void setBoundary75s(List<RatingRange> boundary75s) {
      this.boundary75s = boundary75s;
    }

    public List<RatingRange> getBoundary80s() {
      return boundary80s;
    }

    public void setBoundary80s(List<RatingRange> boundary80s) {
      this.boundary80s = boundary80s;
    }

    public int getTeamSize() {
      return teamSize;
    }

    public void setTeamSize(int team_size) {
      this.teamSize = team_size;
    }

    public int getNumPlayers() {
      return numPlayers;
    }

    public void setNumPlayers(int numPlayers) {
      this.numPlayers = numPlayers;
    }
  }
  private List<MatchmakerQueue> queues;

  public MatchmakerInfoMessage() {
    super(FafServerMessageType.MATCHMAKER_INFO);
  }


  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public List<MatchmakerQueue> getQueues() {
    return queues;
  }

  public void setQueues(List<MatchmakerQueue> queues) {
    this.queues = queues;
  }
}
