package com.faforever.client.teammatchmaking;

import com.faforever.client.api.dto.MatchmakerQueue;
import com.faforever.client.leaderboard.Leaderboard;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;

public class MatchmakingQueue {
  private final IntegerProperty queueId;
  private final StringProperty queueName;
  private final ObjectProperty<Instant> queuePopTime;
  private final IntegerProperty teamSize;
  private final IntegerProperty partiesInQueue;
  private final IntegerProperty playersInQueue;
  private final BooleanProperty joined;
  private final ObjectProperty<MatchingStatus> matchingStatus;
  private final ObjectProperty<Leaderboard> leaderboard;

  public MatchmakingQueue() {
    this.queueId = new SimpleIntegerProperty();
    this.queueName = new SimpleStringProperty();
    this.queuePopTime = new SimpleObjectProperty<>(Instant.now().plus(Duration.ofDays(1)));
    this.teamSize = new SimpleIntegerProperty(0);
    this.partiesInQueue = new SimpleIntegerProperty(0);
    this.playersInQueue = new SimpleIntegerProperty(0);
    this.joined = new SimpleBooleanProperty(false);
    this.matchingStatus = new SimpleObjectProperty<>(null);
    this.leaderboard = new SimpleObjectProperty<>(null);
  }

  public static MatchmakingQueue fromDto(MatchmakerQueue dto) {
    MatchmakingQueue queue = new MatchmakingQueue();
    queue.setQueueId(Integer.parseInt(dto.getId()));
    queue.setQueueName(dto.getTechnicalName());
    queue.setLeaderboard(Leaderboard.fromDto(dto.getLeaderboard()));
    return queue;
  }

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

  public int getQueueId() {
    return queueId.get();
  }

  public void setQueueId(int queueId) {
    this.queueId.set(queueId);
  }

  public IntegerProperty queueIdProperty() {
    return queueId;
  }

  public String getQueueName() {
    return queueName.get();
  }

  public void setQueueName(String queueName) {
    this.queueName.set(queueName);
  }

  public StringProperty queueNameProperty() {
    return queueName;
  }

  public Instant getQueuePopTime() {
    return queuePopTime.get();
  }

  public void setQueuePopTime(Instant queuePopTime) {
    this.queuePopTime.set(queuePopTime);
  }

  public ObjectProperty<Instant> queuePopTimeProperty() {
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

  public int getPartiesInQueue() {
    return partiesInQueue.get();
  }

  public void setPartiesInQueue(int partiesInQueue) {
    this.partiesInQueue.set(partiesInQueue);
  }

  public IntegerProperty partiesInQueueProperty() {
    return partiesInQueue;
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
