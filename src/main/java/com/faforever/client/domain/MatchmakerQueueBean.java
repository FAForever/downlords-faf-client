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
import lombok.ToString;
import lombok.Value;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Value
public class MatchmakerQueueBean extends AbstractEntityBean<MatchmakerQueueBean> {
  StringProperty technicalName = new SimpleStringProperty();
  ObjectProperty<OffsetDateTime> queuePopTime= new SimpleObjectProperty<OffsetDateTime>();
  IntegerProperty teamSize = new SimpleIntegerProperty(0);
  IntegerProperty playersInQueue = new SimpleIntegerProperty(0);
  BooleanProperty joined = new SimpleBooleanProperty(false);
  ObjectProperty<MatchingStatus> matchingStatus = new SimpleObjectProperty<>();
  ObjectProperty<LeaderboardBean> leaderboard = new SimpleObjectProperty<>();
  ObjectProperty<FeaturedModBean> featuredMod = new SimpleObjectProperty<>();

  public void setTimedOutMatchingStatus(MatchingStatus status, Duration timeout, TaskScheduler taskScheduler) {
    setMatchingStatus(status);
    taskScheduler.schedule(() -> {
      if (getMatchingStatus() == status) {
        setMatchingStatus(null);
      }
    }, Instant.now().plus(timeout));
  }

  public enum MatchingStatus {
    MATCH_FOUND, GAME_LAUNCHING, MATCH_CANCELLED
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

  public boolean isJoined() {
    return joined.get();
  }

  public void setJoined(boolean joined) {
    this.joined.set(joined);
  }

  public BooleanProperty joinedProperty() {
    return joined;
  }
}
