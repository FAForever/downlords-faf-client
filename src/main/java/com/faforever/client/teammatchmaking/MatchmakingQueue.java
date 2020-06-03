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
  private IntegerProperty playersInQueue;

  public MatchmakingQueue(String queueName) {
    this.queueName = new SimpleStringProperty(queueName);
    this.queuePopTime = new SimpleObjectProperty<>(Instant.now().plus(Duration.ofDays(1)));
    this.playersInQueue = new SimpleIntegerProperty(0);
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

  public int getPlayersInQueue() {
    return playersInQueue.get();
  }

  public void setPlayersInQueue(int playersInQueue) {
    this.playersInQueue.set(playersInQueue);
  }

  public IntegerProperty playersInQueueProperty() {
    return playersInQueue;
  }
}
