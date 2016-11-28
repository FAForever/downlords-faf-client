package com.faforever.client.replay;

public class ReplayInfoBeanBuilder {

  private final Replay replay;

  private ReplayInfoBeanBuilder() {
    replay = new Replay();
  }

  public static ReplayInfoBeanBuilder create() {
    return new ReplayInfoBeanBuilder();
  }

  public Replay get() {
    return replay;
  }
}
