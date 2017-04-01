package com.faforever.client.game;

import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.util.TimeUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.commons.lang3.StringEscapeUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game {

  private static final String RATING_NUMBER = "\\d+(?:\\.\\d+)?k?";
  private static final Pattern MIN_RATING_PATTERN = Pattern.compile(">\\s*(" + RATING_NUMBER + ")|(" + RATING_NUMBER + ")\\s*\\+");
  private static final Pattern MAX_RATING_PATTERN = Pattern.compile("<\\s*(" + RATING_NUMBER + ")");
  private static final Pattern ABOUT_RATING_PATTERN = Pattern.compile("~\\s*(" + RATING_NUMBER + ")");
  private static final Pattern BETWEEN_RATING_PATTERN = Pattern.compile("(" + RATING_NUMBER + ")\\s*-\\s*(" + RATING_NUMBER + ")");

  private final StringProperty host;
  private final StringProperty title;
  private final StringProperty mapFolderName;
  private final StringProperty featuredMod;
  private final IntegerProperty id;
  private final IntegerProperty numPlayers;
  private final IntegerProperty maxPlayers;
  private final IntegerProperty minRating;
  private final IntegerProperty maxRating;
  private final BooleanProperty passwordProtected;
  private final StringProperty password;
  private final ObjectProperty<GameVisibility> visibility;
  private final ObjectProperty<GameState> status;
  private final ObjectProperty<VictoryCondition> victoryCondition;
  private final ObjectProperty<Instant> startTime;
  /**
   * Maps a sim mod's UID to its name.
   */
  private final MapProperty<String, String> simMods;
  private final MapProperty<String, List<String>> teams;
  /**
   * Maps an index (1,2,3,4...) to a version number. Don't ask me what this index maps to.
   */
  private final MapProperty<String, Integer> featuredModVersions;

  public Game(GameInfoMessage gameInfoMessage) {
    this();
    updateFromGameInfo(gameInfoMessage);
  }

  public Game() {
    id = new SimpleIntegerProperty();
    host = new SimpleStringProperty();
    title = new SimpleStringProperty();
    mapFolderName = new SimpleStringProperty();
    featuredMod = new SimpleStringProperty();
    numPlayers = new SimpleIntegerProperty();
    maxPlayers = new SimpleIntegerProperty();
    minRating = new SimpleIntegerProperty(0);
    maxRating = new SimpleIntegerProperty(3000);
    passwordProtected = new SimpleBooleanProperty();
    password = new SimpleStringProperty();
    victoryCondition = new SimpleObjectProperty<>();
    visibility = new SimpleObjectProperty<>();
    simMods = new SimpleMapProperty<>(FXCollections.observableHashMap());
    teams = new SimpleMapProperty<>(FXCollections.observableHashMap());
    featuredModVersions = new SimpleMapProperty<>(FXCollections.observableHashMap());
    status = new SimpleObjectProperty<>();
    startTime = new SimpleObjectProperty<>();
  }

  public void updateFromGameInfo(GameInfoMessage gameInfoMessage) {
    id.set(gameInfoMessage.getUid());
    host.set(gameInfoMessage.getHost());
    title.set(StringEscapeUtils.unescapeHtml4(gameInfoMessage.getTitle()));
    mapFolderName.set(gameInfoMessage.getMapname());
    featuredMod.set(gameInfoMessage.getFeaturedMod());
    numPlayers.setValue(gameInfoMessage.getNumPlayers());
    maxPlayers.setValue(gameInfoMessage.getMaxPlayers());
    victoryCondition.set(gameInfoMessage.getGameType());
    status.set(gameInfoMessage.getState());
    passwordProtected.set(gameInfoMessage.getPasswordProtected());
    Optional.ofNullable(gameInfoMessage.getLaunchedAt()).ifPresent(aDouble -> startTime.set(
        TimeUtil.fromPythonTime(aDouble.longValue()).toInstant()
    ));

    synchronized (simMods.get()) {
      simMods.clear();
      if (gameInfoMessage.getSimMods() != null) {
        simMods.putAll(gameInfoMessage.getSimMods());
      }
    }

    synchronized (teams.get()) {
      teams.clear();
      if (gameInfoMessage.getTeams() != null) {
        teams.putAll(gameInfoMessage.getTeams());
      }
    }

    synchronized (featuredModVersions.get()) {
      featuredModVersions.clear();
      if (gameInfoMessage.getFeaturedModVersions() != null) {
        featuredModVersions.putAll(gameInfoMessage.getFeaturedModVersions());
      }
    }

    // TODO this can be removed as soon as we get server side support. Until then, let's be hacky
    String titleString = title.get();
    Matcher matcher = BETWEEN_RATING_PATTERN.matcher(titleString);
    if (matcher.find()) {
      minRating.set(parseRating(matcher.group(1)));
      maxRating.set(parseRating(matcher.group(2)));
    } else {
      matcher = MIN_RATING_PATTERN.matcher(titleString);
      if (matcher.find()) {
        if (matcher.group(1) != null) {
          minRating.set(parseRating(matcher.group(1)));
        }
        if (matcher.group(2) != null) {
          minRating.set(parseRating(matcher.group(2)));
        }
        maxRating.set(3000);
      } else {
        matcher = MAX_RATING_PATTERN.matcher(titleString);
        if (matcher.find()) {
          minRating.set(0);
          maxRating.setValue(parseRating(matcher.group(1)));
        } else {
          matcher = ABOUT_RATING_PATTERN.matcher(titleString);
          if (matcher.find()) {
            int rating = parseRating(matcher.group(1));
            minRating.set(rating - 300);
            maxRating.set(rating + 300);
          }
        }
      }
    }
  }

  private int parseRating(String string) {
    try {
      return Integer.parseInt(string);
    } catch (NumberFormatException e) {
      int rating;
      String[] split = string.replace("k", "").split("\\.");
      try {
        rating = Integer.parseInt(split[0]) * 1000;
        if (split.length == 2) {
          rating += Integer.parseInt(split[1]) * 100;
        }
        return rating;
      } catch (NumberFormatException e1) {
        return Integer.MAX_VALUE;
      }
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

  public String getMapFolderName() {
    return mapFolderName.get();
  }

  public void setMapFolderName(String mapFolderName) {
    this.mapFolderName.set(mapFolderName);
  }

  public StringProperty mapFolderNameProperty() {
    return mapFolderName;
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

  public int getId() {
    return id.get();
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public IntegerProperty idProperty() {
    return id;
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

  /**
   * Returns a map of simulation mod UIDs to the mod's name.
   */
  public ObservableMap<String, String> getSimMods() {
    return simMods.get();
  }

  public void setSimMods(ObservableMap<String, String> simMods) {
    this.simMods.set(simMods);
  }

  public MapProperty<String, String> simModsProperty() {
    return simMods;
  }

  /**
   * Maps team names ("1", "2", ...) to a list of player names.
   * <strong>Make sure to synchronize on the return value.</strong>
   */
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
    return id.getValue().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Game
        && id.getValue().equals(((Game) obj).id.getValue());
  }

  public GameVisibility getVisibility() {
    return visibility.get();
  }

  public ObjectProperty<GameVisibility> visibilityProperty() {
    return visibility;
  }

  public boolean getPasswordProtected() {
    return passwordProtected.get();
  }

  public BooleanProperty passwordProtectedProperty() {
    return passwordProtected;
  }

  public String getPassword() {
    return password.get();
  }

  public void setPassword(String password) {
    this.password.set(password);
  }

  public StringProperty passwordProperty() {
    return password;
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
}
