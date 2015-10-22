package com.faforever.client.stats.domain;

public class UnitStatBuilder {

  private UnitStat unitStat;

  private UnitStatBuilder() {
    unitStat = new UnitStat();
  }

  public UnitStatBuilder killed(int killed) {
    unitStat.setKilled(killed);
    return this;
  }

  public UnitStatBuilder built(int built) {
    unitStat.setBuilt(built);
    return this;
  }

  public UnitStatBuilder damageDealt(int damageDealt) {
    unitStat.setDamagedealt(damageDealt);
    return this;
  }

  public UnitStatBuilder damageReceived(int damageReceived) {
    unitStat.setDamagereceived(damageReceived);
    return this;
  }

  public UnitStatBuilder energyCost(int energyCost) {
    unitStat.setEnergycost(energyCost);
    return this;
  }

  public UnitStatBuilder id(Unit unit) {
    unitStat.setId(unit.getId());
    return this;
  }

  public UnitStatBuilder buildTime(int buildTime) {
    unitStat.setBuildtime(buildTime);
    return this;
  }

  public UnitStatBuilder lost(int lost) {
    unitStat.setLost(lost);
    return this;
  }

  public UnitStatBuilder massCost(int massCost) {
    unitStat.setMasscost(massCost);
    return this;
  }

  public UnitStatBuilder unitType(UnitType type) {
    unitStat.setType(type);
    return this;
  }

  public UnitStat get() {
    return unitStat;
  }

  public UnitStatBuilder defaultValues() {
    unitStat.setType(UnitType.UNKNOWN);
    return this;
  }

  public static UnitStatBuilder create() {
    return new UnitStatBuilder();
  }
}
