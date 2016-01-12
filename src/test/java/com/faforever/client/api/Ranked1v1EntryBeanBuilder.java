package com.faforever.client.api;

import com.faforever.client.leaderboard.Ranked1v1EntryBean;

public class Ranked1v1EntryBeanBuilder {

  private Ranked1v1EntryBean ranked1v1EntryBean;

  private Ranked1v1EntryBeanBuilder() {
    ranked1v1EntryBean = new Ranked1v1EntryBean();
  }

  public Ranked1v1EntryBeanBuilder username(String username) {
    ranked1v1EntryBean.setUsername(username);
    return this;
  }

  public Ranked1v1EntryBeanBuilder defaultValues() {
    return this;
  }

  public Ranked1v1EntryBean get() {
    return ranked1v1EntryBean;
  }

  public static Ranked1v1EntryBeanBuilder create() {
    return new Ranked1v1EntryBeanBuilder();
  }
}
