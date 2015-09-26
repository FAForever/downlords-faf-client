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
  private final StringProperty mapTechnicalName;
  private final StringProperty featuredMod;
  private final ObjectProperty<GameAccess> access;
  private final IntegerProperty uid;
  private final IntegerProperty numPlayers;
  private final IntegerProperty maxPlayers;
  private final IntegerProperty minRating;
  private final IntegerProperty maxRating;
  private final ObjectProperty<GameState> status;
  private final ObjectProperty<VictoryCondition> victoryCondition;
  private final ListProperty<Boolean> options;
  /**
   * Maps a sim mod's UID to its name.
   */
  private final MapProperty<String, String> simMods;
  private final MapProperty<String, List<String>> teams;
  /** Maps an index (1,2,3,4...) to a version number. Don't ask me what this index maps to. */
  private final MapProperty<String, Integer> featuredModVersions;

  public GameInfoBean(GameInfo gameInfo) {
    this();
    updateFromGameInfo(gameInfo);
  }

  public GameInfoBean() {
    uid = new SimpleIntegerProperty();
    host = new SimpleStringProperty();
    title = new SimpleStringProperty();
    mapTechnicalName = new SimpleStringProperty();
    featuredMod = new SimpleStringProperty();
    access = new SimpleObjectProperty<>();
    numPlayers = new SimpleIntegerProperty();
    maxPlayers = new SimpleIntegerProperty();
    minRating = new SimpleIntegerProperty();
    maxRating = new SimpleIntegerProperty();
    victoryCondition = new SimpleObjectProperty<>();
    options = new SimpleListProperty<>(FXCollections.observableArrayList());
    simMods = new SimpleMapProperty<>(FXCollections.observableHashMap());
    teams = new SimpleMapProperty<>(FXCollections.observableHashMap());
    featuredModVersions = new SimpleMapProperty<>(FXCollections.observableHashMap());
    status = new SimpleObjectProperty<>();
  }

  public void updateFromGameInfo(GameInfo gameInfo) {
    uid.set(gameInfo.getUid());
    host.set(gameInfo.getHost());
    title.set(StringEscapeUtils.unescapeHtml4(gameInfo.getTitle()));
    access.set(gameInfo.getAccess());
    mapTechnicalName.set(gameInfo.getMapname());
    featuredMod.set(gameInfo.getFeaturedMod());
    numPlayers.setValue(gameInfo.getNumPlayers());
    maxPlayers.setValue(gameInfo.getMaxPlayers());
    victoryCondition.set(gameInfo.getGameType());
    status.set(gameInfo.getState());

    if (gameInfo.getOptions() != null) {
      options.setAll(gameInfo.getOptions());
    }

    simMods.clear();
    if (gameInfo.getSimMods() != null) {
      simMods.putAll(gameInfo.getSimMods());
    }

    teams.clear();
    if (gameInfo.getTeams() != null) {
      teams.putAll(gameInfo.getTeams());
    }

    featuredModVersions.clear();
    if (gameInfo.getFeaturedModVersions() != null) {
      featuredModVersions.putAll(gameInfo.getFeaturedModVersions());
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
      maxRating.set(3000);
    }
  }

  public String getHost() {
    return host.get();
  }

  public void setHost(String host) {
    this.host.set(host);
  }

  public StringProperty hostProperty() {
    return host;
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

  public String getMapTechnicalName() {
    return mapTechnicalName.get();
  }

  public void setMapTechnicalName(String mapTechnicalName) {
    this.mapTechnicalName.set(mapTechnicalName);
  }

  public StringProperty mapTechnicalNameProperty() {
    return mapTechnicalName;
  }

  public String getFeaturedMod() {
    return featuredMod.get();
  }

  public void setFeaturedMod(String featuredMod) {
    this.featuredMod.set(featuredMod);
  }

  public StringProperty featuredModProperty() {
    return featuredMod;
  }

  public GameAccess getAccess() {
    return access.get();
  }

  public void setAccess(GameAccess access) {
    this.access.set(access);
  }

  public ObjectProperty<GameAccess> accessProperty() {
    return access;
  }

  public int getUid() {
    return uid.get();
  }

  public void setUid(int uid) {
    this.uid.set(uid);
  }

  public IntegerProperty uidProperty() {
    return uid;
  }

  public int getNumPlayers() {
    return numPlayers.get();
  }

  public void setNumPlayers(int numPlayers) {
    this.numPlayers.set(numPlayers);
  }

  public IntegerProperty numPlayersProperty() {
    return numPlayers;
  }

  public int getMaxPlayers() {
    return maxPlayers.get();
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers.set(maxPlayers);
  }

  public IntegerProperty maxPlayersProperty() {
    return maxPlayers;
  }

  public int getMinRating() {
    return minRating.get();
  }

  public void setMinRating(int minRating) {
    this.minRating.set(minRating);
  }

  public IntegerProperty minRatingProperty() {
    return minRating;
  }

  public int getMaxRating() {
    return maxRating.get();
  }

  public void setMaxRating(int maxRating) {
    this.maxRating.set(maxRating);
  }

  public IntegerProperty maxRatingProperty() {
    return maxRating;
  }

  public GameState getStatus() {
    return status.get();
  }

  public void setStatus(GameState status) {
    this.status.set(status);
  }

  public ObjectProperty<GameState> statusProperty() {
    return status;
  }

  public VictoryCondition getVictoryCondition() {
    return victoryCondition.get();
  }

  public void setVictoryCondition(VictoryCondition victoryCondition) {
    this.victoryCondition.set(victoryCondition);
  }

  public ObjectProperty<VictoryCondition> victoryConditionProperty() {
    return victoryCondition;
  }

  public ObservableList<Boolean> getOptions() {
    return options.get();
  }

  public void setOptions(ObservableList<Boolean> options) {
    this.options.set(options);
  }

  public ListProperty<Boolean> optionsProperty() {
    return options;
  }

  public ObservableMap<String, String> getSimMods() {
    return simMods.get();
  }

  public void setSimMods(ObservableMap<String, String> simMods) {
    this.simMods.set(simMods);
  }

  public MapProperty<String, String> simModsProperty() {
    return simMods;
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

  public ObservableMap<String, Integer> getFeaturedModVersions() {
    return featuredModVersions.get();
  }

  public void setFeaturedModVersions(ObservableMap<String, Integer> featuredModVersions) {
    this.featuredModVersions.set(featuredModVersions);
  }

  public MapProperty<String, Integer> featuredModVersionsProperty() {
    return featuredModVersions;
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

  public static Pattern getRatingPattern() {
    return RATING_PATTERN;
  }
}
