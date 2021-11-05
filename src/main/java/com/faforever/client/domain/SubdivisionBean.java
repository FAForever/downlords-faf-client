package com.faforever.client.domain;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class SubdivisionBean extends AbstractEntityBean<SubdivisionBean> {
  IntegerProperty leagueSeasonId = new SimpleIntegerProperty();
  @ToString.Include
  StringProperty NameKey = new SimpleStringProperty();
  StringProperty descriptionKey = new SimpleStringProperty();
  IntegerProperty index = new SimpleIntegerProperty();
  IntegerProperty highestScore = new SimpleIntegerProperty();
  IntegerProperty maxRating = new SimpleIntegerProperty();
  IntegerProperty minRating = new SimpleIntegerProperty();
  ObjectProperty<DivisionBean> division = new SimpleObjectProperty<>();

  public int getLeagueSeasonId() {
    return leagueSeasonId.get();
  }

  public void setLeagueSeasonId(int id) {
    this.leagueSeasonId.set(id);
  }

  public IntegerProperty leagueSeasonIdProperty() {
    return leagueSeasonId;
  }

  public String getNameKey() {
    return NameKey.get();
  }

  public void setNameKey(String nameKey) {
    this.NameKey.set(nameKey);
  }

  public StringProperty nameKeyProperty() {
    return NameKey;
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

  public int getHighestScore() {
    return highestScore.get();
  }

  public void setHighestScore(int highestScore) {
    this.highestScore.set(highestScore);
  }

  public IntegerProperty highestScoreProperty() {
    return highestScore;
  }

  public int getMaxRating() {
    return maxRating.get();
  }

  public void setMaxRating(int maxRating) {
    this.maxRating.set(maxRating);
  }

  public IntegerProperty maxRatingProperty() {
    return maxRating;
  }

  public int getMinRating() {
    return minRating.get();
  }

  public void setMinRating(int minRating) {
    this.minRating.set(minRating);
  }

  public IntegerProperty minRatingProperty() {
    return minRating;
  }

  public DivisionBean getDivision() {
    return division.get();
  }

  public void setDivision(DivisionBean division) {
    this.division.set(division);
  }

  public ObjectProperty<DivisionBean> divisionProperty() {
    return division;
  }

  public String getDivisionI18nKey() {
    return String.format("leagues.divisionName.%s", getDivision().getIndex());
  }

  public URL getImageUrl() {
    String url = String.format("https://content.faforever.com/divisions/icons/%s%s.png",
        getDivision().getNameKey(), getNameKey());
    return getUrlFromString(url);
  }

  public URL getMediumImageUrl() {
    String url = String.format("https://content.faforever.com/divisions/icons/medium/%s%s_medium.png",
        getDivision().getNameKey(), getNameKey());
    return getUrlFromString(url);
  }

  public URL getSmallImageUrl() {
    String url = String.format("https://content.faforever.com/divisions/icons/small/%s%s_small.png",
        getDivision().getNameKey(), getNameKey());
    return getUrlFromString(url);
  }

  private URL getUrlFromString(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      log.warn("Unable to load image due to invalid fileName {}", url, e);
      return null;
    }
  }
}
