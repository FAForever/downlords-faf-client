package com.faforever.client.replay;

public class ReplayInfoBeanBuilder {

  private final ReplayInfoBean replayInfoBean;

  private ReplayInfoBeanBuilder() {
    replayInfoBean = new ReplayInfoBean();
  }

  public ReplayInfoBean get() {
    return replayInfoBean;
  }

  public static ReplayInfoBeanBuilder create() {
    return new ReplayInfoBeanBuilder();
  }
}
