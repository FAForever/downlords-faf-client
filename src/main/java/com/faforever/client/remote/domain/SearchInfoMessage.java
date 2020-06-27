package com.faforever.client.remote.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class SearchInfoMessage extends FafServerMessage {

  @SerializedName("queue_name")
  private String queueName;
  private MatchmakingState state;

  public SearchInfoMessage() {
    super(FafServerMessageType.SEARCH_INFO);
  }

  @Data
  public static class PartyMember {
    private Integer player;
    private Boolean ready;
    private List<Boolean> factions;
  }
}
