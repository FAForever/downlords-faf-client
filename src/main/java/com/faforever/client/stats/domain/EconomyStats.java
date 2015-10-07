package com.faforever.client.stats.domain;

import com.google.common.annotations.VisibleForTesting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class EconomyStats {

  @XmlElement(name = "Energy")
  private EconomyStat energy;
  @XmlElement(name = "Mass")
  private EconomyStat mass;

  public EconomyStat getEnergy() {
    return energy;
  }

  @VisibleForTesting
  public void setEnergy(EconomyStat energy) {
    this.energy = energy;
  }

  public EconomyStat getMass() {
    return mass;
  }

  @VisibleForTesting
  public void setMass(EconomyStat mass) {
    this.mass = mass;
  }
}
