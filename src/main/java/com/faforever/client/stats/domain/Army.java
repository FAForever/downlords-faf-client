package com.faforever.client.stats.domain;

import com.google.common.annotations.VisibleForTesting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Army {

  @XmlAttribute
  private int index;

  @XmlAttribute
  private String name;

  @XmlElement(name = "Unit")
  @XmlElementWrapper(name = "UnitStats")
  private List<UnitStat> unitStats;

  @XmlElement(name = "Category")
  @XmlElementWrapper(name = "SummaryStats")
  private List<SummaryStat> summaryStats;

  @XmlElement(name = "EconomyStats")
  private EconomyStats economyStats;

  public int getIndex() {
    return index;
  }

  @VisibleForTesting
  public void setIndex(int index) {
    this.index = index;
  }

  public String getName() {
    return name;
  }

  @VisibleForTesting
  public void setName(String name) {
    this.name = name;
  }

  public List<UnitStat> getUnitStats() {
    return unitStats;
  }

  @VisibleForTesting
  public void setUnitStats(List<UnitStat> unitStats) {
    this.unitStats = unitStats;
  }

  public List<SummaryStat> getSummaryStats() {
    return summaryStats;
  }

  @VisibleForTesting
  public void setSummaryStats(List<SummaryStat> summaryStats) {
    this.summaryStats = summaryStats;
  }

  public EconomyStats getEconomyStats() {
    return economyStats;
  }

  @VisibleForTesting
  public void setEconomyStats(EconomyStats economyStats) {
    this.economyStats = economyStats;
  }
}
