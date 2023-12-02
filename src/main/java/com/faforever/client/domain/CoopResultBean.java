package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;

import java.time.Duration;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CoopResultBean {
  @EqualsAndHashCode.Include
  private final ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  private final BooleanProperty secondaryObjectives = new SimpleBooleanProperty();
  private final ObjectProperty<Duration> duration = new SimpleObjectProperty<>();
  private final IntegerProperty ranking = new SimpleIntegerProperty();
  private final IntegerProperty playerCount = new SimpleIntegerProperty();
  private final ObjectProperty<ReplayBean> replay = new SimpleObjectProperty<>();

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public ObjectProperty<Integer> idProperty() {
    return id;
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
