package com.faforever.client.domain.server;

import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.teammatchmaking.MatchingStatus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class MatchmakerQueueInfo {
  @EqualsAndHashCode.Include
  @ToString.Include
  private final ObjectProperty<Integer> id = new SimpleObjectProperty<>();
  @ToString.Include
  private final StringProperty technicalName = new SimpleStringProperty();
  private final ObjectProperty<OffsetDateTime> queuePopTime = new SimpleObjectProperty<>();
  private final IntegerProperty teamSize = new SimpleIntegerProperty(0);
  private final IntegerProperty playersInQueue = new SimpleIntegerProperty(0);
  private final IntegerProperty activeGames = new SimpleIntegerProperty(0);
  private final BooleanProperty selected = new SimpleBooleanProperty(true);
  private final ObjectProperty<MatchingStatus> matchingStatus = new SimpleObjectProperty<>();
  private final ObjectProperty<Leaderboard> leaderboard = new SimpleObjectProperty<>();

  public Integer getId() {
    return id.get();
  }

  public ObjectProperty<Integer> idProperty() {
    return id;
  }

  public void setId(Integer id) {
    this.id.set(id);
  }

  public MatchingStatus getMatchingStatus() {
    return matchingStatus.get();
  }

  public ObjectProperty<MatchingStatus> matchingStatusProperty() {
    return matchingStatus;
  }

  public void setMatchingStatus(MatchingStatus matchingStatus) {
    this.matchingStatus.set(matchingStatus);
  }

  public String getTechnicalName() {
    return technicalName.get();
  }

  public void setTechnicalName(String technicalName) {
    this.technicalName.set(technicalName);
  }

  public StringProperty technicalNameProperty() {
    return technicalName;
  }

  public OffsetDateTime getQueuePopTime() {
    return queuePopTime.get();
  }

  public void setQueuePopTime(OffsetDateTime queuePopTime) {
    this.queuePopTime.set(queuePopTime);
  }

  public ObjectProperty<OffsetDateTime> queuePopTimeProperty() {
    return queuePopTime;
  }

  public Leaderboard getLeaderboard() {
    return leaderboard.get();
  }

  public void setLeaderboard(Leaderboard leaderboard) {
    this.leaderboard.set(leaderboard);
  }

  public ObjectProperty<Leaderboard> leaderboardProperty() {
    return leaderboard;
  }

  public int getTeamSize() {
    return teamSize.get();
  }

  public void setTeamSize(int teamSize) {
    this.teamSize.set(teamSize);
  }

  public IntegerProperty teamSizeProperty() {
    return teamSize;
  }

  public int getPlayersInQueue() {
    return playersInQueue.get();
  }

  public void setPlayersInQueue(int playersInQueue) {
    this.playersInQueue.set(playersInQueue);
  }

  public IntegerProperty playersInQueueProperty() {
    return playersInQueue;
  }

  public int getActiveGames() {
    return activeGames.get();
  }

  public IntegerProperty activeGamesProperty() {
    return activeGames;
  }

  public void setActiveGames(int activeGames) {
    this.activeGames.set(activeGames);
  }

  public boolean isSelected() {
    return selected.get();
  }

  public void setSelected(boolean selected) {
    this.selected.set(selected);
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }
}
