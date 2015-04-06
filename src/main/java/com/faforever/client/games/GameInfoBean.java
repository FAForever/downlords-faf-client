package com.faforever.client.games;

import com.faforever.client.legacy.message.GameInfoMessage;
import com.faforever.client.legacy.message.GameStatus;
import com.faforever.client.legacy.message.GameType;
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
  private StringProperty access;
  private StringProperty mapName;
  private StringProperty featuredMod;
  private ObjectProperty<Integer> uid;
  private ObjectProperty<Integer> minRanking;
  private ObjectProperty<Integer> maxRanking;
  private ObjectProperty<Integer> numPlayers;
  private ObjectProperty<Integer> maxPlayers;
  private ObjectProperty<GameStatus> status;
  private ObjectProperty<GameType> gameType;
  private ObjectProperty<BigDecimal> gameTime;
  private ListProperty<Boolean> options;
  private MapProperty<String, String> simMods;
  private MapProperty<String, List<String>> teams;
  private MapProperty<String, Integer> featuredModVersions;

  public GameInfoBean(GameInfoMessage gameInfoMessage) {
    uid = new SimpleObjectProperty<>(gameInfoMessage.uid);
    host = new SimpleStringProperty(gameInfoMessage.host);
    title = new SimpleStringProperty(StringEscapeUtils.unescapeHtml4(gameInfoMessage.title));
    access = new SimpleStringProperty(gameInfoMessage.access);
    mapName = new SimpleStringProperty(gameInfoMessage.mapname);
    featuredMod = new SimpleStringProperty(gameInfoMessage.featuredMod);
    minRanking = new SimpleObjectProperty<>(gameInfoMessage.minRanking);
    maxRanking = new SimpleObjectProperty<>(gameInfoMessage.maxRanking);
    numPlayers = new SimpleObjectProperty<>(gameInfoMessage.numPlayers);
    maxPlayers = new SimpleObjectProperty<>(gameInfoMessage.maxPlayers);
    gameType = new SimpleObjectProperty<>(gameInfoMessage.gameType);
    gameTime = new SimpleObjectProperty<>(gameInfoMessage.gameTime);
    options = new SimpleListProperty<>(FXCollections.observableArrayList(gameInfoMessage.options));
    simMods = new SimpleMapProperty<>(FXCollections.observableMap(gameInfoMessage.simMods));
    teams = new SimpleMapProperty<>(FXCollections.observableMap(gameInfoMessage.teams));
    featuredModVersions = new SimpleMapProperty<>(FXCollections.observableMap(gameInfoMessage.featuredModVersions));
  }

  public void updateFromGameInfo(GameInfoMessage gameInfoMessage) {
    uid.set(gameInfoMessage.uid);
    host.set(gameInfoMessage.host);
    title.set(StringEscapeUtils.unescapeHtml4(gameInfoMessage.title));
    access.set(gameInfoMessage.access);
    mapName.set(gameInfoMessage.mapname);
    featuredMod.set(gameInfoMessage.featuredMod);
    minRanking.set(gameInfoMessage.minRanking);
    maxRanking.set(gameInfoMessage.maxRanking);
    numPlayers.set(gameInfoMessage.numPlayers);
    maxPlayers.set(gameInfoMessage.maxPlayers);
    gameType.set(gameInfoMessage.gameType);
    gameTime.set(gameInfoMessage.gameTime);
    options.setAll(gameInfoMessage.options);
    simMods.putAll(gameInfoMessage.simMods);
    teams.putAll(gameInfoMessage.teams);
    featuredModVersions.putAll(gameInfoMessage.featuredModVersions);
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

  public String getAccess() {
    return access.get();
  }

  public StringProperty accessProperty() {
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

  public GameStatus getStatus() {
    return status.get();
  }

  public ObjectProperty<GameStatus> statusProperty() {
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
