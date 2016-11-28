package com.faforever.client.patch;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.eclipse.jgit.lib.ProgressMonitor;

public class PropertiesProgressMonitor implements ProgressMonitor {
  private final IntegerProperty totalTasks;
  private final BooleanProperty completed;
  private final IntegerProperty totalWork;
  private final BooleanProperty cancelled;
  private final StringProperty title;
  private final IntegerProperty workUnitsDone;
  private final IntegerProperty tasksDone;

  public PropertiesProgressMonitor() {
    totalTasks = new SimpleIntegerProperty();
    completed = new SimpleBooleanProperty();
    totalWork = new SimpleIntegerProperty();
    cancelled = new SimpleBooleanProperty();
    title = new SimpleStringProperty();
    workUnitsDone = new SimpleIntegerProperty();
    tasksDone = new SimpleIntegerProperty();
  }

  public int getTotalTasks() {
    return totalTasks.get();
  }

  public IntegerProperty totalTasksProperty() {
    return totalTasks;
  }

  public boolean isCompleted() {
    return completed.get();
  }

  public BooleanProperty completedProperty() {
    return completed;
  }

  public int getTotalWork() {
    return totalWork.get();
  }

  public IntegerProperty totalWorkProperty() {
    return totalWork;
  }

  public boolean getCancelled() {
    return cancelled.get();
  }

  public BooleanProperty cancelledProperty() {
    return cancelled;
  }

  public String getTitle() {
    return title.get();
  }

  public StringProperty titleProperty() {
    return title;
  }

  public int getWorkUnitsDone() {
    return workUnitsDone.get();
  }

  public IntegerProperty workUnitsDoneProperty() {
    return workUnitsDone;
  }

  @Override
  public void start(int totalTasks) {
    this.totalTasks.setValue(totalTasks);
    this.tasksDone.setValue(0);
    workUnitsDone.setValue(0);
  }

  @Override
  public void beginTask(String title, int totalWork) {
    workUnitsDone.setValue(0);
    this.totalWork.setValue(totalWork);
    this.title.setValue(title);
  }

  @Override
  public void update(int completed) {
    this.workUnitsDone.setValue(Math.min(totalWork.get(), workUnitsDone.getValue() + completed));
  }

  @Override
  public void endTask() {
    tasksDone.setValue(Math.min(totalTasks.get(), tasksDone.getValue() + 1));
  }

  @Override
  public boolean isCancelled() {
    return cancelled.get();
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled.set(cancelled);
  }

  public int getTasksDone() {
    return tasksDone.get();
  }

  public IntegerProperty tasksDoneProperty() {
    return tasksDone;
  }
}
