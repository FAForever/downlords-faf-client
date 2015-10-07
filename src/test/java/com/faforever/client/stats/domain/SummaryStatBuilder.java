package com.faforever.client.stats.domain;

public final class SummaryStatBuilder {

  private SummaryStat summaryStat;

  private SummaryStatBuilder() {
    summaryStat = new SummaryStat();
  }

  public SummaryStatBuilder type(UnitCategory type) {
    summaryStat.setType(type);
    return this;
  }

  public SummaryStatBuilder built(int built) {
    summaryStat.setBuilt(built);
    return this;
  }

  public SummaryStatBuilder killed(int killed) {
    summaryStat.setKilled(killed);
    return this;
  }

  public SummaryStat get() {
    return summaryStat;
  }

  public static SummaryStatBuilder create() {
    return new SummaryStatBuilder();
  }
}
