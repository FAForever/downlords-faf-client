package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Duration;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CoopResultBean {
  @EqualsAndHashCode.Include
  ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  StringProperty playerNames = new SimpleStringProperty();
  BooleanProperty secondaryObjectives = new SimpleBooleanProperty();
  ObjectProperty<Duration> duration = new SimpleObjectProperty<>();
  IntegerProperty ranking = new SimpleIntegerProperty();
  IntegerProperty playerCount = new SimpleIntegerProperty();
  ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public ObjectProperty<Integer> idProperty() {
    return id;
  }

  public String getPlayerNames() {
    return playerNames.get();
  }

  public StringProperty playerNamesProperty() {
    return playerNames;
  }

  public void setPlayerNames(String playerNames) {
    this.playerNames.set(playerNames);
  }

  public boolean getSecondaryObjectives() {
    return secondaryObjectives.get();
  }

  public BooleanProperty secondaryObjectivesProperty() {
    return secondaryObjectives;
  }

  public void setSecondaryObjectives(boolean secondaryObjectives) {
    this.secondaryObjectives.set(secondaryObjectives);
  }

  public Duration getDuration() {
    return duration.get();
  }

  public ObjectProperty<Duration> durationProperty() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration.set(duration);
  }

  public int getRanking() {
    return ranking.get();
  }

  public IntegerProperty rankingProperty() {
    return ranking;
  }

  public void setRanking(int ranking) {
    this.ranking.set(ranking);
  }

  public int getPlayerCount() {
    return playerCount.get();
  }

  public IntegerProperty playerCountProperty() {
    return playerCount;
  }

  public void setPlayerCount(int playerCount) {
    this.playerCount.set(playerCount);
  }

  public ReplayBean getReplay() {
    return replay.get();
  }

  public ObjectProperty<ReplayBean> replayProperty() {
    return replay;
  }

  public void setReplay(ReplayBean replay) {
    this.replay.set(replay);
  }
}
