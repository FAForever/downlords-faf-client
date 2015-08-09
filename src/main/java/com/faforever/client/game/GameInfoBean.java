package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.VictoryCondition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameInfoBean {

  private static final Pattern MIN_RATING_PATTERN = Pattern.compile(">?\\s?(\\d+)\\s?\\+?");
  private static final Pattern MAX_RATING_PATTERN = Pattern.compile("<\\s?(\\d+)?");
  private static final Pattern ABOUT_RATING_PATTERN = Pattern.compile("~?(\\d+)");
  private static final Pattern BETWEEN_RATING_PATTERN = Pattern.compile("(\\d+)\\s?-\\s?(\\d+)");
  private static final Pattern RATING_PATTERN = Pattern.compile("([<>+~](?:\\d\\.?\\d?k|\\d{3,4})|(?:\\d\\.?\\d?k|\\d{3,4})[<>+]|(?:\\d\\.?\\d?k|\\d{1,4})\\s?-\\s?(?:\\d\\.?\\d?k|\\d{3,4}))");

  private final StringProperty host;
  private final StringProperty title;
  private final StringProperty mapName;
  private final StringProperty featuredMod;
  private final ObjectProperty<GameAccess> access;
  private final IntegerProperty uid;
  private final IntegerProperty numPlayers;
  private final IntegerProperty maxPlayers;
  private final IntegerProperty minRating;
  private final IntegerProperty maxRating;
  private final ObjectProperty<GameState> status;
  private final ObjectProperty<VictoryCondition> gameType;
  private final ListProperty<Boolean> options;
  private final MapProperty<String, String> simMods;
  private final MapProperty<String, List<String>> teams;
  private final MapProperty<String, Integer> featuredModVersions;

  public GameInfoBean(GameInfo gameInfo) {
    this();
    updateFromGameInfo(gameInfo);
  }

  public GameInfoBean() {
    uid = new SimpleIntegerProperty();
    host = new SimpleStringProperty();
    title = new SimpleStringProperty();
    mapName = new SimpleStringProperty();
    featuredMod = new SimpleStringProperty();
    access = new SimpleObjectProperty<>();
    numPlayers = new SimpleIntegerProperty();
    maxPlayers = new SimpleIntegerProperty();
    minRating = new SimpleIntegerProperty();
    maxRating = new SimpleIntegerProperty();
    gameType = new SimpleObjectProperty<>();
    options = new SimpleListProperty<>(FXCollections.observableArrayList());
    simMods = new SimpleMapProperty<>(FXCollections.observableHashMap());
    teams = new SimpleMapProperty<>(FXCollections.observableHashMap());
    featuredModVersions = new SimpleMapProperty<>(FXCollections.observableHashMap());
    status = new SimpleObjectProperty<>();
  }

  public void updateFromGameInfo(GameInfo gameInfo) {
    uid.set(gameInfo.uid);
    host.set(gameInfo.host);
    title.set(StringEscapeUtils.unescapeHtml4(gameInfo.title));
    access.set(gameInfo.access);
    mapName.set(gameInfo.mapname);
    featuredMod.set(gameInfo.featuredMod);
    numPlayers.setValue(gameInfo.numPlayers);
    maxPlayers.setValue(gameInfo.maxPlayers);
    gameType.set(gameInfo.gameType);
    status.set(gameInfo.state);

    if (gameInfo.options != null) {
      options.setAll(gameInfo.options);
    }

    simMods.clear();
    if (gameInfo.simMods != null) {
      simMods.putAll(gameInfo.simMods);
    }

    teams.clear();
    if (gameInfo.teams != null) {
      teams.putAll(gameInfo.teams);
    }

    featuredModVersions.clear();
    if (gameInfo.featuredModVersions != null) {
      featuredModVersions.putAll(gameInfo.featuredModVersions);
    }

    // TODO as this can be removed as soon as we get server side support. Until then, let's be hacky
    Matcher matcher = RATING_PATTERN.matcher(title.get());
    if (matcher.find()) {
      String ratingString = matcher.group(1);
      matcher = BETWEEN_RATING_PATTERN.matcher(ratingString);
      if (matcher.find()) {
        minRating.set(Integer.parseInt(matcher.group(1)));
        maxRating.set(Integer.parseInt(matcher.group(2)));
      } else {
        matcher = MIN_RATING_PATTERN.matcher(ratingString);
        if (matcher.find()) {
          minRating.set(Integer.parseInt(matcher.group(1)));
          maxRating.set(3000);
        } else {
          matcher = MAX_RATING_PATTERN.matcher(ratingString);
          if (matcher.find()) {
            maxRating.setValue(Integer.parseInt(ratingString));
          }
        }
      }
    } else {
      maxRating.set(2500);
    }
  }

  public static Pattern getRatingPattern() {
    return RATING_PATTERN;
  }

  public String getHost() {
    return host.get();
  }

  public StringProperty hostProperty() {
    return host;
  }

  public void setHost(String host) {
    this.host.set(host);
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

  public String getMapName() {
    return mapName.get();
  }

  public StringProperty mapNameProperty() {
    return mapName;
  }

  public void setMapName(String mapName) {
    this.mapName.set(mapName);
  }

  public String getFeaturedMod() {
    return featuredMod.get();
  }

  public StringProperty featuredModProperty() {
    return featuredMod;
  }

  public void setFeaturedMod(String featuredMod) {
    this.featuredMod.set(featuredMod);
  }

  public GameAccess getAccess() {
    return access.get();
  }

  public ObjectProperty<GameAccess> accessProperty() {
    return access;
  }

  public void setAccess(GameAccess access) {
    this.access.set(access);
  }

  public int getUid() {
    return uid.get();
  }

  public IntegerProperty uidProperty() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid.set(uid);
  }

  public int getNumPlayers() {
    return numPlayers.get();
  }

  public IntegerProperty numPlayersProperty() {
    return numPlayers;
  }

  public void setNumPlayers(int numPlayers) {
    this.numPlayers.set(numPlayers);
  }

  public int getMaxPlayers() {
    return maxPlayers.get();
  }

  public IntegerProperty maxPlayersProperty() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers.set(maxPlayers);
  }

  public int getMinRating() {
    return minRating.get();
  }

  public IntegerProperty minRatingProperty() {
    return minRating;
  }

  public void setMinRating(int minRating) {
    this.minRating.set(minRating);
  }

  public int getMaxRating() {
    return maxRating.get();
  }

  public IntegerProperty maxRatingProperty() {
    return maxRating;
  }

  public void setMaxRating(int maxRating) {
    this.maxRating.set(maxRating);
  }

  public GameState getStatus() {
    return status.get();
  }

  public ObjectProperty<GameState> statusProperty() {
    return status;
  }

  public void setStatus(GameState status) {
    this.status.set(status);
  }

  public VictoryCondition getGameType() {
    return gameType.get();
  }

  public ObjectProperty<VictoryCondition> gameTypeProperty() {
    return gameType;
  }

  public void setGameType(VictoryCondition gameType) {
    this.gameType.set(gameType);
  }

  public ObservableList<Boolean> getOptions() {
    return options.get();
  }

  public ListProperty<Boolean> optionsProperty() {
    return options;
  }

  public void setOptions(ObservableList<Boolean> options) {
    this.options.set(options);
  }

  public ObservableMap<String, String> getSimMods() {
    return simMods.get();
  }

  public MapProperty<String, String> simModsProperty() {
    return simMods;
  }

  public void setSimMods(ObservableMap<String, String> simMods) {
    this.simMods.set(simMods);
  }

  public ObservableMap<String, List<String>> getTeams() {
    return teams.get();
  }

  public MapProperty<String, List<String>> teamsProperty() {
    return teams;
  }

  public void setTeams(ObservableMap<String, List<String>> teams) {
    this.teams.set(teams);
  }

  public ObservableMap<String, Integer> getFeaturedModVersions() {
    return featuredModVersions.get();
  }

  public MapProperty<String, Integer> featuredModVersionsProperty() {
    return featuredModVersions;
  }

  public void setFeaturedModVersions(ObservableMap<String, Integer> featuredModVersions) {
    this.featuredModVersions.set(featuredModVersions);
  }

  @Override
  public int hashCode() {
    return uid.getValue().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof GameInfoBean
        && uid.getValue().equals(((GameInfoBean) obj).uid.getValue());
  }
}
