package com.faforever.client.remote.domain;

import java.util.List;

public class SocialMessage extends FafServerMessage {

  private List<Integer> friends;
  private List<Integer> foes;
  private List<String> channels;

  public List<Integer> getFriends() {
    return friends;
  }

  public List<Integer> getFoes() {
    return foes;
  }

  /**
   * List of channel names to join.
   */
  public List<String> getChannels() {
    return channels;
  }

  public void setChannels(List<String> channels) {
    this.channels = channels;
  }
}
