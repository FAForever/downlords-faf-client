package com.faforever.client.replay;

import java.util.List;

public class ReplayServerObject {

  private ReplayAction action;
  private List<ServerReplayInfo> replays;

  public ReplayAction getAction() {
    return action;
  }

  public void setAction(ReplayAction action) {
    this.action = action;
  }

  public List<ServerReplayInfo> getReplays() {
    return replays;
  }

  public void setReplays(List<ServerReplayInfo> replays) {
    this.replays = replays;
  }
}
