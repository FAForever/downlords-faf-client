package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MatchmakerQueueBean extends AbstractEntityBean<MatchmakerQueueBean> {
  @ToString.Include
  StringProperty technicalName = new SimpleStringProperty();
  ObjectProperty<OffsetDateTime> queuePopTime= new SimpleObjectProperty<OffsetDateTime>();
  IntegerProperty teamSize = new SimpleIntegerProperty(0);
  IntegerProperty playersInQueue = new SimpleIntegerProperty(0);
  IntegerProperty activeGames = new SimpleIntegerProperty(0);
  BooleanProperty selected = new SimpleBooleanProperty(true);
  ObjectProperty<MatchingStatus> matchingStatus = new SimpleObjectProperty<>();
  ObjectProperty<LeaderboardBean> leaderboard = new SimpleObjectProperty<>();
  ObjectProperty<FeaturedModBean> featuredMod = new SimpleObjectProperty<>();

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

  public LeaderboardBean getLeaderboard() {
    return leaderboard.get();
  }

  public void setLeaderboard(LeaderboardBean leaderboard) {
    this.leaderboard.set(leaderboard);
  }

  public ObjectProperty<LeaderboardBean> leaderboardProperty() {
    return leaderboard;
  }

  public FeaturedModBean getFeaturedMod() {
    return featuredMod.get();
  }

  public ObjectProperty<FeaturedModBean> featuredModProperty() {
    return featuredMod;
  }

  public void setFeaturedMod(FeaturedModBean featuredMod) {
    this.featuredMod.set(featuredMod);
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
