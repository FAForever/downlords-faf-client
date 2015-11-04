package com.faforever.client.stats.domain;

import java.util.ArrayList;

import static com.faforever.client.stats.domain.UnitCategory.AIR;
import static com.faforever.client.stats.domain.UnitCategory.ENGINEER;
import static com.faforever.client.stats.domain.UnitCategory.LAND;
import static com.faforever.client.stats.domain.UnitCategory.NAVAL;
import static com.faforever.client.stats.domain.UnitCategory.TECH1;
import static com.faforever.client.stats.domain.UnitCategory.TECH2;
import static com.faforever.client.stats.domain.UnitCategory.TECH3;

public class ArmyBuilder {

  private Army army;

  private ArmyBuilder(String name) {
    army = new Army();
    army.setName(name);
    army.setSummaryStats(new ArrayList<>());
    army.setUnitStats(new ArrayList<>());
    army.setEconomyStats(new EconomyStats());
  }

  public ArmyBuilder unitStat(UnitStat unitStat) {
    army.getUnitStats().add(unitStat);
    return this;
  }

  public Army get() {
    return army;
  }

  public ArmyBuilder defaultValues() {
    return massStat(EconomyStatBuilder.create().get())
        .energyStat(EconomyStatBuilder.create().get())
        .summaryStat(SummaryStatBuilder.create(AIR).built(0).killed(0).get())
        .summaryStat(SummaryStatBuilder.create(LAND).built(0).killed(0).get())
        .summaryStat(SummaryStatBuilder.create(NAVAL).built(0).killed(0).get())
        .summaryStat(SummaryStatBuilder.create(ENGINEER).built(0).killed(0).get())
        .summaryStat(SummaryStatBuilder.create(TECH1).built(0).killed(0).get())
        .summaryStat(SummaryStatBuilder.create(TECH2).built(0).killed(0).get())
        .summaryStat(SummaryStatBuilder.create(TECH3).built(0).killed(0).get());
  }

  public ArmyBuilder summaryStat(SummaryStat summaryStat) {
    army.getSummaryStats().add(summaryStat);
    return this;
  }

  public ArmyBuilder energyStat(EconomyStat economyStat) {
    army.getEconomyStats().setEnergy(economyStat);
    return this;
  }

  public ArmyBuilder massStat(EconomyStat economyStat) {
    army.getEconomyStats().setMass(economyStat);
    return this;
  }

  public static ArmyBuilder create(String name) {
    return new ArmyBuilder(name);
  }
}
