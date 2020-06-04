package com.faforever.client.teammatchmaking;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.Duration;
import java.time.Instant;

public class MatchmakingQueue {
  private StringProperty queueName;
  private ObjectProperty<Instant> queuePopTime;
  private IntegerProperty teamSize;
  private IntegerProperty partiesInQueue;

  public MatchmakingQueue(String queueName) {
    this.queueName = new SimpleStringProperty(queueName);
    this.queuePopTime = new SimpleObjectProperty<>(Instant.now().plus(Duration.ofDays(1)));
    this.teamSize = new SimpleIntegerProperty(0);
    this.partiesInQueue = new SimpleIntegerProperty(0);
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
}
