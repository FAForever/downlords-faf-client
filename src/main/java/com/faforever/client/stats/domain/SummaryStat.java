package com.faforever.client.stats.domain;

import com.google.common.annotations.VisibleForTesting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class SummaryStat {

  @XmlAttribute
  private UnitCategory type;
  @XmlAttribute
  private int built;
  @XmlAttribute
  private int killed;

  public UnitCategory getType() {
    return type;
  }

  @VisibleForTesting
  public void setType(UnitCategory type) {
    this.type = type;
  }

  public int getBuilt() {
    return built;
  }

  @VisibleForTesting
  public void setBuilt(int built) {
    this.built = built;
  }

  public int getKilled() {
    return killed;
  }

  @VisibleForTesting
  public void setKilled(int killed) {
    this.killed = killed;
  }
}
