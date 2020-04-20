package com.faforever.client.rankedmatch;

import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.RatingRange;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MatchmakerInfoMessage extends FafServerMessage {

  public static class MatchmakerQueue {

    private QueueName queueName;
    private String queuePopTime;
    @SerializedName("boundary_75s")
    private List<RatingRange> boundary75s;
    @SerializedName("boundary_80s")
    private List<RatingRange> boundary80s;

    public static enum QueueName {
      @SerializedName("ladder1v1")
      LADDER_1V1
    }

    public MatchmakerQueue(QueueName queueName, String queuePopTime, List<RatingRange> boundary75s, List<RatingRange> boundary80s) {
      this.queueName = queueName;
      this.queuePopTime = queuePopTime;
      this.boundary75s = boundary75s;
      this.boundary80s = boundary80s;
    }

    public QueueName getQueueName() {
      return queueName;
    }

    public void setQueueName(QueueName queueName) {
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
  }

  private String action;
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
