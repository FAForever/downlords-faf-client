package com.faforever.client.legacy.domain;

import java.util.List;

public class SocialMessage extends FafServerMessage {

  private List<Integer> friends;
  private List<Integer> foes;
  private List<String> autojoin;

  public List<Integer> getFriends() {
    return friends;
  }

  public List<Integer> getFoes() {
    return foes;
  }

  public List<String> getAutojoin() {
    return autojoin;
  }

  /**
   * List of channel names to join.
   */
  public List<String> getAutoJoin() {
    return autojoin;
  }

  public void setAutoJoin(List<String> autojoin) {
    this.autojoin = autojoin;
  }
}
