package com.faforever.client.stats.domain;

import com.google.common.annotations.VisibleForTesting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class EconomyStat {

  @XmlAttribute
  private double produced;
  @XmlAttribute
  private double consumed;
  @XmlAttribute
  private double storage;

  public double getProduced() {
    return produced;
  }

  @VisibleForTesting
  public void setProduced(double produced) {
    this.produced = produced;
  }

  public double getConsumed() {
    return consumed;
  }

  @VisibleForTesting
  public void setConsumed(double consumed) {
    this.consumed = consumed;
  }

  public double getStorage() {
    return storage;
  }

  @VisibleForTesting
  public void setStorage(double storage) {
    this.storage = storage;
  }
}
