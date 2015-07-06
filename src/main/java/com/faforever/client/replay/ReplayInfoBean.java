package com.faforever.client.replay;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.commons.lang3.StringEscapeUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public class ReplayInfoBean {

  private final IntegerProperty id;
  private final StringProperty title;
  private final MapProperty<String, List<String>> teams;
  private final ObjectProperty<Instant> startTime;
  private final ObjectProperty<Instant> endTime;
  private final StringProperty gameType;
  private final StringProperty map;
  private final ObjectProperty<Path> replayFile;

  public ReplayInfoBean(String title) {
    this();
    this.map.set(title);
  }

  public ReplayInfoBean() {
    id = new SimpleIntegerProperty();
    title = new SimpleStringProperty();
    teams = new SimpleMapProperty<>(FXCollections.observableHashMap());
    startTime = new SimpleObjectProperty<>();
    endTime = new SimpleObjectProperty<>();
    gameType = new SimpleStringProperty();
    map = new SimpleStringProperty();
    replayFile = new SimpleObjectProperty<>();
  }

  public ReplayInfoBean(LocalReplayInfo replayInfo, Path replayFile) {
    this();
    id.set(replayInfo.uid);
    title.set(StringEscapeUtils.unescapeHtml4(replayInfo.title));
    startTime.set(fromPythonTime(replayInfo.gameTime));
    endTime.set(fromPythonTime(replayInfo.gameEnd));
    gameType.set(replayInfo.featuredMod);
    map.set(replayInfo.mapname);
    this.replayFile.set(replayFile);
    if (replayInfo.teams != null) {
      teams.putAll(replayInfo.teams);
    }
  }

  public ReplayInfoBean(ServerReplayInfo replayInfo) {
    this();
    id.setValue(replayInfo.id);
    gameType.setValue(replayInfo.mod);
    map.setValue(replayInfo.map);
    startTime.setValue(Instant.ofEpochMilli(replayInfo.start * 1000));
    endTime.setValue(Instant.ofEpochMilli(replayInfo.end * 1000));
  }

  public void setTeams(ObservableMap<String, List<String>> teams) {
    this.teams.set(teams);
  }

  public void setGameType(String gameType) {
    this.gameType.set(gameType);
  }

  public Path getReplayFile() {
    return replayFile.get();
  }

  public ObjectProperty<Path> replayFileProperty() {
    return replayFile;
  }

  public void setReplayFile(Path replayFile) {
    this.replayFile.set(replayFile);
  }

  public String getTitle() {
    return title.get();
  }

  public StringProperty titleProperty() {
    return title;
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public ObservableMap<String, List<String>> getTeams() {
    return teams.get();
  }

  public MapProperty<String, List<String>> teamsProperty() {
    return teams;
  }

  public int getId() {
    return id.get();
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public Instant getStartTime() {
    return startTime.get();
  }

  public ObjectProperty<Instant> startTimeProperty() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime.set(startTime);
  }

  public Instant getEndTime() {
    return endTime.get();
  }

  public ObjectProperty<Instant> endTimeProperty() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime.set(endTime);
  }

  public String getGameType() {
    return gameType.get();
  }

  public StringProperty gameTypeProperty() {
    return gameType;
  }

  public String getMap() {
    return map.get();
  }

  public StringProperty mapProperty() {
    return map;
  }

  public void setMap(String map) {
    this.map.set(map);
  }

  private static Instant fromPythonTime(double time) {
    return Instant.ofEpochMilli((long) (time * 1000));
  }
}
