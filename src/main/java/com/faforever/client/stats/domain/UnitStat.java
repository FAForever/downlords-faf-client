package com.faforever.client.stats.domain;

import com.google.common.annotations.VisibleForTesting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class UnitStat {

  @XmlAttribute
  private String id;

  @XmlAttribute
  @XmlJavaTypeAdapter(UnitTypeAdapter.class)
  private UnitType type;

  @XmlAttribute
  private int built;

  @XmlAttribute
  private int lost;

  @XmlAttribute
  private int killed;

  @XmlAttribute
  private double damagedealt;

  @XmlAttribute
  private double damagereceived;

  @XmlAttribute
  private double masscost;

  @XmlAttribute
  private double energycost;

  @XmlAttribute
  private double buildtime;

  public String getId() {
    return id;
  }

  @VisibleForTesting
  public void setId(String id) {
    this.id = id;
  }

  public UnitType getType() {
    return type;
  }

  @VisibleForTesting
  public void setType(UnitType type) {
    this.type = type;
  }

  public int getBuilt() {
    return built;
  }

  @VisibleForTesting
  public void setBuilt(int built) {
    this.built = built;
  }

  public int getLost() {
    return lost;
  }

  @VisibleForTesting
  public void setLost(int lost) {
    this.lost = lost;
  }

  public int getKilled() {
    return killed;
  }

  @VisibleForTesting
  public void setKilled(int killed) {
    this.killed = killed;
  }

  public double getDamagedealt() {
    return damagedealt;
  }

  @VisibleForTesting
  public void setDamagedealt(double damagedealt) {
    this.damagedealt = damagedealt;
  }

  public double getDamagereceived() {
    return damagereceived;
  }

  @VisibleForTesting
  public void setDamagereceived(double damagereceived) {
    this.damagereceived = damagereceived;
  }

  public double getMasscost() {
    return masscost;
  }

  @VisibleForTesting
  public void setMasscost(double masscost) {
    this.masscost = masscost;
  }

  public double getEnergycost() {
    return energycost;
  }

  @VisibleForTesting
  public void setEnergycost(double energycost) {
    this.energycost = energycost;
  }

  public double getBuildtime() {
    return buildtime;
  }

  @VisibleForTesting
  public void setBuildtime(double buildtime) {
    this.buildtime = buildtime;
  }

  /**
   * The XML's "built" actually means "survived", this method adds "lost" and "built" in order to get the real "built"
   * count.
   */
  public int getRealBuilt() {
    return lost + built;
  }
}
