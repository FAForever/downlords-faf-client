package com.faforever.client.game;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.GameType;
import com.faforever.client.remote.domain.VictoryCondition;
import com.faforever.client.util.TimeUtil;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.commons.lang3.StringEscapeUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Game {
  public static final String OBSERVERS_TEAM = "-1";
  public static final String NO_TEAM = "1";

  private final StringProperty host = new SimpleStringProperty();
  private final StringProperty title = new SimpleStringProperty();
  private final StringProperty mapFolderName = new SimpleStringProperty();
  private final StringProperty featuredMod = new SimpleStringProperty();
  private final IntegerProperty id = new SimpleIntegerProperty();
  private final IntegerProperty numPlayers = new SimpleIntegerProperty();
  private final IntegerProperty maxPlayers = new SimpleIntegerProperty();
  private final DoubleProperty averageRating = new SimpleDoubleProperty();
  private final StringProperty ratingType = new SimpleStringProperty();
  private final ObjectProperty<Integer> minRating = new SimpleObjectProperty<>(null);
  private final ObjectProperty<Integer> maxRating = new SimpleObjectProperty<>(null);
  private final BooleanProperty passwordProtected = new SimpleBooleanProperty();
  private final StringProperty password = new SimpleStringProperty();
  private final ObjectProperty<GameVisibility> visibility = new SimpleObjectProperty<>();
  private final ObjectProperty<GameStatus> status = new SimpleObjectProperty<>();
  private final ObjectProperty<VictoryCondition> victoryCondition = new SimpleObjectProperty<>();
  private final ObjectProperty<Instant> startTime = new SimpleObjectProperty<>();
  private final BooleanProperty enforceRating = new SimpleBooleanProperty(false);
  private final ObjectProperty<GameType> gameType = new SimpleObjectProperty<>();
  /**
   * Maps a sim mod's UID to its name.
   */
  private final ObjectProperty<Map<String, String>> simMods = new SimpleObjectProperty<>(Map.of());
  private final ObjectProperty<Map<String, List<String>>> teams = new SimpleObjectProperty<>(Map.of());
  /**
   * Maps an index (1,2,3,4...) to a version number. Don't ask me what this index maps to.
   */
  private final MapProperty<String, Integer> featuredModVersions = new SimpleMapProperty<>(FXCollections.observableHashMap());

  private InvalidationListener gameStatusListener;
  private InvalidationListener hostListener;
  private InvalidationListener teamsListener;

  public void updateFromLobbyServer(com.faforever.client.remote.domain.GameInfoMessage gameInfoMessage) {
    setId(gameInfoMessage.getUid());
    setHost(gameInfoMessage.getHost());
    setTitle(StringEscapeUtils.unescapeHtml4(gameInfoMessage.getTitle()));
    setMapFolderName(gameInfoMessage.getMapname());
    setFeaturedMod(gameInfoMessage.getFeaturedMod());
    setNumPlayers(gameInfoMessage.getNumPlayers() - gameInfoMessage.getTeams().getOrDefault(OBSERVERS_TEAM, List.of()).size());
    setMaxPlayers(gameInfoMessage.getMaxPlayers());
    Optional.ofNullable(gameInfoMessage.getLaunchedAt()).ifPresent(aDouble -> setStartTime(
        TimeUtil.fromPythonTime(aDouble.longValue()).toInstant()
    ));
    setStatus(gameInfoMessage.getState());
    setPasswordProtected(gameInfoMessage.getPasswordProtected());
    setGameType(gameInfoMessage.getGameType());
    setRatingType(gameInfoMessage.getRatingType());
    setSimMods(gameInfoMessage.getSimMods());
    setTeams(gameInfoMessage.getTeams());
    setMinRating(gameInfoMessage.getRatingMin());
    setMaxRating(gameInfoMessage.getRatingMax());
    setEnforceRating(gameInfoMessage.getEnforceRatingRange());
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

  public double getAverageRating() {
    return averageRating.get();
  }

  public DoubleProperty averageRatingProperty() {
    return averageRating;
  }

  public void setAverageRating(double averageRating) {
    this.averageRating.set(averageRating);
  }

  public String getRatingType() {
    return ratingType.get();
  }

  public void setRatingType(String ratingType) {
    this.ratingType.set(ratingType);
  }

  public StringProperty ratingTypeProperty() {
    return ratingType;
  }

  public Integer getMinRating() {
    return minRating.get();
  }

  public void setMinRating(Integer minRating) {
    this.minRating.set(minRating);
  }

  public ObjectProperty<Integer> minRatingProperty() {
    return minRating;
  }

  public Integer getMaxRating() {
    return maxRating.get();
  }

  public void setMaxRating(Integer maxRating) {
    this.maxRating.set(maxRating);
  }

  public ObjectProperty<Integer> maxRatingProperty() {
    return maxRating;
  }

  public void setEnforceRating(boolean enforceRating) {
    this.enforceRating.set(enforceRating);
  }

  public boolean getEnforceRating() {
    return enforceRating.getValue();
  }

  public GameStatus getStatus() {
    return status.get();
  }

  public void setStatus(GameStatus status) {
    this.status.set(status);
  }

  public ObjectProperty<GameStatus> statusProperty() {
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

  public GameType getGameType() {
    return gameType.get();
  }

  public void setGameType(GameType gameType) {
    this.gameType.set(gameType);
  }

  public ObjectProperty<GameType> gameTypeProperty() {
    return gameType;
  }

  /**
   * Returns an unmodifiable map of simulation mod UIDs to the mod's name
   */
  public Map<String, String> getSimMods() {
    return simMods.get();
  }

  public void setSimMods(Map<String, String> simMods) {
    this.simMods.set(Collections.unmodifiableMap(simMods));
  }

  public ObjectProperty<Map<String, String>> simModsProperty() {
    return simMods;
  }

  /**
   * Returns an unmodifiable map that maps team names ("1", "2", ...) to a list of player names.
   */
  public Map<String, List<String>> getTeams() {
    return teams.get();
  }

  public void setTeams(Map<String, List<String>> teams) {
    this.teams.set(Collections.unmodifiableMap(teams));
  }

  public ObjectProperty<Map<String, List<String>>> teamsProperty() {
    return teams;
  }

  public ObservableMap<String, Integer> getFeaturedModVersions() {
    return featuredModVersions.get();
  }

  public void setFeaturedModVersions(Map<String, Integer> featuredModVersions) {
    this.featuredModVersions.set(FXCollections.observableMap(featuredModVersions));
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

  public void setVisibility(GameVisibility visibility) {
    this.visibility.setValue(visibility);
  }

  public GameVisibility getVisibility() {
    return visibility.get();
  }

  public ObjectProperty<GameVisibility> visibilityProperty() {
    return visibility;
  }

  public boolean isPasswordProtected() {
    return passwordProtected.get();
  }

  public BooleanProperty passwordProtectedProperty() {
    return passwordProtected;
  }

  public void setPasswordProtected(boolean passwordProtected) {
    this.passwordProtected.set(passwordProtected);
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

  public void setGameStatusListener(InvalidationListener listener) {
    if (gameStatusListener != null) {
      JavaFxUtil.removeListener(status, listener);
    }
    gameStatusListener = listener;
    if (gameStatusListener != null) {
      JavaFxUtil.addAndTriggerListener(status, listener);
    }
  }

  public void setHostListener(InvalidationListener listener) {
    if (hostListener != null) {
      JavaFxUtil.removeListener(host, listener);
    }
    hostListener = listener;
    if (hostListener != null) {
      JavaFxUtil.addAndTriggerListener(host, listener);
    }
  }

  public void setTeamsListener(InvalidationListener listener) {
    if (teamsListener != null) {
      JavaFxUtil.removeListener(teams, listener);
    }
    teamsListener = listener;
    if (teamsListener != null) {
      JavaFxUtil.addAndTriggerListener(teams, listener);
    }
  }

  public void removeListeners() {
    if (gameStatusListener != null) {
      JavaFxUtil.removeListener(status, gameStatusListener);
    }
    gameStatusListener = null;
    if (hostListener != null) {
      JavaFxUtil.removeListener(host, hostListener);
    }
    hostListener = null;
    if (teamsListener != null) {
      JavaFxUtil.removeListener(teams, teamsListener);
    }
    teamsListener = null;
  }

  @Override
  public String toString() {
    return "Game{" +
        "title=" + title.get() +
        ", id=" + id.get() +
        ", status=" + status.get() +
        '}';
  }
}
