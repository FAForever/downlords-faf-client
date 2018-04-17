package com.faforever.client.coop;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.Duration;

public class CoopResult {
  private final StringProperty id;
  private final ObjectProperty<Duration> duration;
  private final StringProperty playerNames;
  private final BooleanProperty secondaryObjectives;
  private final IntegerProperty playerCount;

  /** This field is not provided by the API but must be enriched instead. */
  private final IntegerProperty ranking;

  public CoopResult() {
    id = new SimpleStringProperty();
    duration = new SimpleObjectProperty<>();
    playerNames = new SimpleStringProperty();
    secondaryObjectives = new SimpleBooleanProperty();
    playerCount = new SimpleIntegerProperty();
    ranking = new SimpleIntegerProperty();
  }

  public static CoopResult fromDto(com.faforever.client.api.dto.CoopResult dto) {
    CoopResult result = new CoopResult();
    result.setId(dto.getId());
    result.setDuration(dto.getDuration());
    result.setPlayerCount(dto.getPlayerCount());
    result.setPlayerNames(dto.getPlayerNames());
    result.setSecondaryObjectives(dto.isSecondaryObjectives());
    return result;
  }

  public String getId() {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public StringProperty idProperty() {
    return id;
  }

  public Duration getDuration() {
    return duration.get();
  }

  public void setDuration(Duration duration) {
    this.duration.set(duration);
  }

  public ObjectProperty<Duration> durationProperty() {
    return duration;
  }

  public String getPlayerNames() {
    return playerNames.get();
  }

  public void setPlayerNames(String playerNames) {
    this.playerNames.set(playerNames);
  }

  public StringProperty playerNamesProperty() {
    return playerNames;
  }

  public boolean isSecondaryObjectives() {
    return secondaryObjectives.get();
  }

  public void setSecondaryObjectives(boolean secondaryObjectives) {
    this.secondaryObjectives.set(secondaryObjectives);
  }

  public BooleanProperty secondaryObjectivesProperty() {
    return secondaryObjectives;
  }

  public int getPlayerCount() {
    return playerCount.get();
  }

  public void setPlayerCount(int playerCount) {
    this.playerCount.set(playerCount);
  }

  public IntegerProperty playerCountProperty() {
    return playerCount;
  }

  public int getRanking() {
    return ranking.get();
  }

  public void setRanking(int ranking) {
    this.ranking.set(ranking);
  }

  public IntegerProperty rankingProperty() {
    return ranking;
  }
}
