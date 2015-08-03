package com.faforever.client.rankedmatch;

public class StopSearchRanked1v1Message extends MatchMakerMessage {

  public StopSearchRanked1v1Message() {
    state = "askingtostop";
    mod = "matchmaker";
  }
}
