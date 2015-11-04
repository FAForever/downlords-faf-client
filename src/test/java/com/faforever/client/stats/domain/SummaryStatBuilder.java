package com.faforever.client.stats.domain;

public final class SummaryStatBuilder {

  private SummaryStat summaryStat;

  private SummaryStatBuilder(UnitCategory type) {
    summaryStat = new SummaryStat(type);
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

  public static SummaryStatBuilder create(UnitCategory type) {
    return new SummaryStatBuilder(type);
  }
}
