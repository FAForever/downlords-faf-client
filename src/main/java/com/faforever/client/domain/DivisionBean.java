package com.faforever.client.domain;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class DivisionBean extends AbstractEntityBean<DivisionBean> {
  StringProperty descriptionKey = new SimpleStringProperty();
  IntegerProperty index = new SimpleIntegerProperty();
  @ToString.Include
  StringProperty nameKey = new SimpleStringProperty();
  ObjectProperty<LeagueSeasonBean> leagueSeason = new SimpleObjectProperty<>();

  public LeagueSeasonBean getLeagueSeason() {
    return leagueSeason.get();
  }

  public void setLeagueSeason(LeagueSeasonBean id) {
    this.leagueSeason.set(id);
  }

  public ObjectProperty<LeagueSeasonBean> leagueSeasonProperty() {
    return leagueSeason;
  }

  public String getNameKey() {
    return nameKey.get();
  }

  public void setNameKey(String nameKey) {
    this.nameKey.set(nameKey);
  }

  public StringProperty nameKeyProperty() {
    return nameKey;
  }

  public String getDescriptionKey() {
    return descriptionKey.get();
  }

  public void setDescriptionKey(String descriptionKey) {
    this.descriptionKey.set(descriptionKey);
  }

  public StringProperty descriptionKeyProperty() {
    return descriptionKey;
  }

  public int getIndex() {
    return index.get();
  }

  public void setIndex(int index) {
    this.index.set(index);
  }

  public IntegerProperty indexProperty() {
    return index;
  }

}
