package com.faforever.client.rankedmatch;

import com.faforever.client.game.RatingRange;
import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;

import java.util.List;

public class MatchmakerMessage extends FafServerMessage {

  public enum GameQuality {
    QUALITY_80("boundary_80s"), QUALITY_75("boundary_75s");

    private String key;

    GameQuality(String key) {
      this.key = key;
    }
  }

  public class MatchmakerQueue {

    private String queueName;
    private List<RatingRange> ratingRanges;

    public String getQueueName() {
      return queueName;
    }

    public List<RatingRange> getRatingRanges() {
      return ratingRanges;
    }
  }

  private String action;
  private List<MatchmakerQueue> queues;

  public MatchmakerMessage() {
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
