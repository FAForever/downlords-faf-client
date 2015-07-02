package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.VictoryCondition;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.List;

public class GameInfoBean {

  private StringProperty host;
  private StringProperty title;
  private StringProperty mapName;
  private StringProperty featuredMod;
  private ObjectProperty<GameAccess> access;
  private ObjectProperty<Integer> uid;
  private ObjectProperty<Integer> numPlayers;
  private ObjectProperty<Integer> maxPlayers;
  private ObjectProperty<GameState> status;
  private ObjectProperty<VictoryCondition> gameType;
  private ListProperty<Boolean> options;
  private MapProperty<String, String> simMods;
  private MapProperty<String, List<String>> teams;
  private MapProperty<String, Integer> featuredModVersions;

  public GameInfoBean() {
    uid = new SimpleObjectProperty<>();
    host = new SimpleStringProperty();
    title = new SimpleStringProperty();
    mapName = new SimpleStringProperty();
    featuredMod = new SimpleStringProperty();
    access = new SimpleObjectProperty<>();
    numPlayers = new SimpleObjectProperty<>();
    maxPlayers = new SimpleObjectProperty<>();
    gameType = new SimpleObjectProperty<>();
    options = new SimpleListProperty<>(FXCollections.observableArrayList());
    simMods = new SimpleMapProperty<>(FXCollections.observableHashMap());
    teams = new SimpleMapProperty<>(FXCollections.observableHashMap());
    featuredModVersions = new SimpleMapProperty<>(FXCollections.observableHashMap());
    status = new SimpleObjectProperty<>();
  }

  public GameInfoBean(GameInfo gameInfo) {
    this();
    updateFromGameInfo(gameInfo);
  }

  public void updateFromGameInfo(GameInfo gameInfo) {
    uid.set(gameInfo.uid);
    host.set(gameInfo.host);
    title.set(StringEscapeUtils.unescapeHtml4(gameInfo.title));
    access.set(gameInfo.access);
    mapName.set(gameInfo.mapname);
    featuredMod.set(gameInfo.featuredMod);
    numPlayers.set(gameInfo.numPlayers);
    maxPlayers.set(gameInfo.maxPlayers);
    gameType.set(gameInfo.gameType);
    options.setAll(gameInfo.options);
    simMods.putAll(gameInfo.simMods);
    teams.putAll(gameInfo.teams);
    featuredModVersions.putAll(gameInfo.featuredModVersions);
    status.set(gameInfo.state);
  }

  public int getUid() {
    return uid.get();
  }

  public ObjectProperty<Integer> uidProperty() {
    return uid;
  }

  public String getHost() {
    return host.get();
  }

  public StringProperty hostProperty() {
    return host;
  }

  public String getTitle() {
    return title.get();
  }

  public StringProperty titleProperty() {
    return title;
  }

  public GameAccess getAccess() {
    return access.get();
  }

  public ObjectProperty<GameAccess> accessProperty() {
    return access;
  }

  public String getMapName() {
    return mapName.get();
  }

  public StringProperty mapNameProperty() {
    return mapName;
  }

  public String getFeaturedMod() {
    return featuredMod.get();
  }

  public StringProperty featuredModProperty() {
    return featuredMod;
  }

  public Integer getNumPlayers() {
    return numPlayers.get();
  }

  public ObjectProperty<Integer> numPlayersProperty() {
    return numPlayers;
  }

  public Integer getMaxPlayers() {
    return maxPlayers.get();
  }

  public ObjectProperty<Integer> maxPlayersProperty() {
    return maxPlayers;
  }

  public GameState getStatus() {
    return status.get();
  }

  public ObjectProperty<GameState> statusProperty() {
    return status;
  }

  public VictoryCondition getGameType() {
    return gameType.get();
  }

  public ObjectProperty<VictoryCondition> gameTypeProperty() {
    return gameType;
  }

  public ObservableList<Boolean> getOptions() {
    return options.get();
  }

  public ListProperty<Boolean> optionsProperty() {
    return options;
  }

  public ObservableMap<String, String> getSimMods() {
    return simMods.get();
  }

  public MapProperty<String, String> simModsProperty() {
    return simMods;
  }

  public ObservableMap<String, List<String>> getTeams() {
    return teams.get();
  }

  public MapProperty<String, List<String>> teamsProperty() {
    return teams;
  }

  public ObservableMap<String, Integer> getFeaturedModVersions() {
    return featuredModVersions.get();
  }

  public MapProperty<String, Integer> featuredModVersionsProperty() {
    return featuredModVersions;
  }

  public void setHost(String host) {
    this.host.set(host);
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public void setMapName(String mapName) {
    this.mapName.set(mapName);
  }

  public void setFeaturedMod(String featuredMod) {
    this.featuredMod.set(featuredMod);
  }

  public void setAccess(GameAccess access) {
    this.access.set(access);
  }

  public void setUid(Integer uid) {
    this.uid.set(uid);
  }

  public void setNumPlayers(Integer numPlayers) {
    this.numPlayers.set(numPlayers);
  }

  public void setMaxPlayers(Integer maxPlayers) {
    this.maxPlayers.set(maxPlayers);
  }

  public void setStatus(GameState status) {
    this.status.set(status);
  }

  public void setGameType(VictoryCondition gameType) {
    this.gameType.set(gameType);
  }


  public void setOptions(ObservableList<Boolean> options) {
    this.options.set(options);
  }

  public void setSimMods(ObservableMap<String, String> simMods) {
    this.simMods.set(simMods);
  }

  public void setTeams(ObservableMap<String, List<String>> teams) {
    this.teams.set(teams);
  }

  public void setFeaturedModVersions(ObservableMap<String, Integer> featuredModVersions) {
    this.featuredModVersions.set(featuredModVersions);
  }
}
