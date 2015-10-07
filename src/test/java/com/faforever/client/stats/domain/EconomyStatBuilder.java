package com.faforever.client.stats.domain;

public class EconomyStatBuilder {

  private final EconomyStat economyStat;

  private EconomyStatBuilder() {
    economyStat = new EconomyStat();
  }

  public EconomyStat get() {
    return economyStat;
  }

  public EconomyStatBuilder produced(double produced) {
    economyStat.setProduced(produced);
    return this;
  }

  public EconomyStatBuilder consumed(double consumed) {
    economyStat.setConsumed(consumed);
    return this;
  }

  public EconomyStatBuilder storage(double storage) {
    economyStat.setConsumed(storage);
    return this;
  }

  public static EconomyStatBuilder create() {
    return new EconomyStatBuilder();
  }
}
