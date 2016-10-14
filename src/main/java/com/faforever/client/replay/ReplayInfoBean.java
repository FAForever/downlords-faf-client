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

import static com.faforever.client.util.TimeUtil.fromPythonTime;

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
    id.set(replayInfo.getUid());
    title.set(StringEscapeUtils.unescapeHtml4(replayInfo.getTitle()));
    startTime.set(fromPythonTime(replayInfo.getGameTime() > 0 ? replayInfo.getGameTime() : replayInfo.getLaunchedAt()));
    endTime.set(fromPythonTime(replayInfo.getGameEnd()));
    gameType.set(replayInfo.getFeaturedMod());
    map.set(replayInfo.getMapname());
    this.replayFile.set(replayFile);
    if (replayInfo.getTeams() != null) {
      teams.putAll(replayInfo.getTeams());
    }
  }

  public ReplayInfoBean(ServerReplayInfo replayInfo) {
    this();
    id.setValue(replayInfo.id);
    title.setValue(replayInfo.name);
    gameType.setValue(replayInfo.mod);
    map.setValue(replayInfo.map);
    startTime.setValue(Instant.ofEpochMilli(replayInfo.start * 1000));
    endTime.setValue(Instant.ofEpochMilli(replayInfo.end * 1000));
  }

  public Path getReplayFile() {
    return replayFile.get();
  }

  public void setReplayFile(Path replayFile) {
    this.replayFile.set(replayFile);
  }

  public ObjectProperty<Path> replayFileProperty() {
    return replayFile;
  }

  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public ObservableMap<String, List<String>> getTeams() {
    return teams.get();
  }

  public void setTeams(ObservableMap<String, List<String>> teams) {
    this.teams.set(teams);
  }

  public MapProperty<String, List<String>> teamsProperty() {
    return teams;
  }

  public int getId() {
    return id.get();
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public Instant getStartTime() {
    return startTime.get();
  }

  public void setStartTime(Instant startTime) {
    this.startTime.set(startTime);
  }

  public ObjectProperty<Instant> startTimeProperty() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime.get();
  }

  public void setEndTime(Instant endTime) {
    this.endTime.set(endTime);
  }

  public ObjectProperty<Instant> endTimeProperty() {
    return endTime;
  }

  public String getGameType() {
    return gameType.get();
  }

  public void setGameType(String gameType) {
    this.gameType.set(gameType);
  }

  public StringProperty gameTypeProperty() {
    return gameType;
  }

  public String getMap() {
    return map.get();
  }

  public void setMap(String map) {
    this.map.set(map);
  }

  public StringProperty mapProperty() {
    return map;
  }
}
