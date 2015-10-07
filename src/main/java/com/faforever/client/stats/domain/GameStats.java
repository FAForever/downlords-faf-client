package com.faforever.client.stats.domain;

import com.google.common.annotations.VisibleForTesting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GameStats")
public class GameStats {

  @XmlElement(name = "Army")
  private List<Army> armies;

  public List<Army> getArmies() {
    return armies;
  }

  @VisibleForTesting
  public void setArmies(List<Army> armies) {
    this.armies = armies;
  }
}
