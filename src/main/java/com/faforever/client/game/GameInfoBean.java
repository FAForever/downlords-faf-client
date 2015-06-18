package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameType;
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

import java.math.BigDecimal;
import java.util.List;

public class GameInfoBean {

  private StringProperty host;
  private StringProperty title;
  private StringProperty mapName;
  private StringProperty featuredMod;
  private ObjectProperty<GameAccess> access;
  private ObjectProperty<Integer> uid;
  private ObjectProperty<Integer> minRanking;
  private ObjectProperty<Integer> maxRanking;
  private ObjectProperty<Integer> numPlayers;
  private ObjectProperty<Integer> maxPlayers;
  private ObjectProperty<GameState> status;
  private ObjectProperty<GameType> gameType;
  private ObjectProperty<BigDecimal> gameTime;
  private ListProperty<Boolean> options;
  private MapProperty<String, String> simMods;
  private MapProperty<String, List<String>> teams;
  private MapProperty<String, Integer> featuredModVersions;

  public GameInfoBean(GameInfo gameInfo) {
    uid = new SimpleObjectProperty<>(gameInfo.uid);
    host = new SimpleStringProperty(gameInfo.host);
    title = new SimpleStringProperty(StringEscapeUtils.unescapeHtml4(gameInfo.title));
    mapName = new SimpleStringProperty(gameInfo.mapname);
    featuredMod = new SimpleStringProperty(gameInfo.featuredMod);
    access = new SimpleObjectProperty<>(gameInfo.access);
    minRanking = new SimpleObjectProperty<>(gameInfo.minRanking);
    maxRanking = new SimpleObjectProperty<>(gameInfo.maxRanking);
    numPlayers = new SimpleObjectProperty<>(gameInfo.numPlayers);
    maxPlayers = new SimpleObjectProperty<>(gameInfo.maxPlayers);
    gameType = new SimpleObjectProperty<>(gameInfo.gameType);
    gameTime = new SimpleObjectProperty<>(gameInfo.gameTime);
    options = new SimpleListProperty<>(FXCollections.observableArrayList(gameInfo.options));
    simMods = new SimpleMapProperty<>(FXCollections.observableMap(gameInfo.simMods));
    teams = new SimpleMapProperty<>(FXCollections.observableMap(gameInfo.teams));
    featuredModVersions = new SimpleMapProperty<>(FXCollections.observableMap(gameInfo.featuredModVersions));
  }

  public void updateFromGameInfo(GameInfo gameInfo) {
    uid.set(gameInfo.uid);
    host.set(gameInfo.host);
    title.set(StringEscapeUtils.unescapeHtml4(gameInfo.title));
    access.set(gameInfo.access);
    mapName.set(gameInfo.mapname);
    featuredMod.set(gameInfo.featuredMod);
    minRanking.set(gameInfo.minRanking);
    maxRanking.set(gameInfo.maxRanking);
    numPlayers.set(gameInfo.numPlayers);
    maxPlayers.set(gameInfo.maxPlayers);
    gameType.set(gameInfo.gameType);
    gameTime.set(gameInfo.gameTime);
    options.setAll(gameInfo.options);
    simMods.putAll(gameInfo.simMods);
    teams.putAll(gameInfo.teams);
    featuredModVersions.putAll(gameInfo.featuredModVersions);
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

  public Integer getMinRanking() {
    return minRanking.get();
  }

  public ObjectProperty<Integer> minRankingProperty() {
    return minRanking;
  }

  public Integer getMaxRanking() {
    return maxRanking.get();
  }

  public ObjectProperty<Integer> maxRankingProperty() {
    return maxRanking;
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

  public GameType getGameType() {
    return gameType.get();
  }

  public ObjectProperty<GameType> gameTypeProperty() {
    return gameType;
  }

  public BigDecimal getGameTime() {
    return gameTime.get();
  }

  public ObjectProperty<BigDecimal> gameTimeProperty() {
    return gameTime;
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
}
