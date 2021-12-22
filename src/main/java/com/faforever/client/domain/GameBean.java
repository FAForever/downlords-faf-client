package com.faforever.client.domain;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.commons.api.dto.VictoryCondition;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GameVisibility;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GameBean {
  public static final String OBSERVERS_TEAM = "-1";
  public static final String NO_TEAM = "1";

  private final StringProperty host = new SimpleStringProperty();
  private final StringProperty title = new SimpleStringProperty();
  private final StringProperty mapFolderName = new SimpleStringProperty();
  private final StringProperty featuredMod = new SimpleStringProperty();
  @EqualsAndHashCode.Include
  private final IntegerProperty id = new SimpleIntegerProperty();
  private final IntegerProperty numPlayers = new SimpleIntegerProperty();
  private final IntegerProperty maxPlayers = new SimpleIntegerProperty();
  private final DoubleProperty averageRating = new SimpleDoubleProperty();
  private final StringProperty leaderboard = new SimpleStringProperty();
  private final ObjectProperty<Integer> ratingMin = new SimpleObjectProperty<>(null);
  private final ObjectProperty<Integer> ratingMax = new SimpleObjectProperty<>(null);
  private final BooleanProperty passwordProtected = new SimpleBooleanProperty();
  private final StringProperty password = new SimpleStringProperty();
  private final ObjectProperty<GameVisibility> visibility = new SimpleObjectProperty<>();
  private final ObjectProperty<GameStatus> status = new SimpleObjectProperty<>();
  private final ObjectProperty<VictoryCondition> victoryCondition = new SimpleObjectProperty<>();
  private final ObjectProperty<OffsetDateTime> startTime = new SimpleObjectProperty<>();
  private final BooleanProperty enforceRating = new SimpleBooleanProperty(false);
  private final ObjectProperty<GameType> gameType = new SimpleObjectProperty<>();
  /**
   * Maps a sim mod's UID to its name.
   */
  private final ObjectProperty<Map<String, String>> simMods = new SimpleObjectProperty<>(Map.of());
  private final ObjectProperty<Map<String, List<String>>> teams = new SimpleObjectProperty<>(Map.of());

  private InvalidationListener gameStatusListener;
  private InvalidationListener hostListener;
  private InvalidationListener teamsListener;

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

  public Integer getId() {
    return id.get();
  }

  public void setId(Integer id) {
    this.id.setValue(id);
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public Integer getNumPlayers() {
    return numPlayers.get();
  }

  public void setNumPlayers(Integer numPlayers) {
    this.numPlayers.setValue(numPlayers);
  }

  public IntegerProperty numPlayersProperty() {
    return numPlayers;
  }

  public Integer getMaxPlayers() {
    return maxPlayers.get();
  }

  public void setMaxPlayers(Integer maxPlayers) {
    this.maxPlayers.setValue(maxPlayers);
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

  public String getLeaderboard() {
    return leaderboard.get();
  }

  public void setLeaderboard(String leaderboard) {
    this.leaderboard.set(leaderboard);
  }

  public StringProperty leaderboardProperty() {
    return leaderboard;
  }

  public Integer getRatingMin() {
    return ratingMin.get();
  }

  public void setRatingMin(Integer ratingMin) {
    this.ratingMin.set(ratingMin);
  }

  public ObjectProperty<Integer> ratingMinProperty() {
    return ratingMin;
  }

  public Integer getRatingMax() {
    return ratingMax.get();
  }

  public void setRatingMax(Integer ratingMax) {
    this.ratingMax.set(ratingMax);
  }

  public ObjectProperty<Integer> ratingMaxProperty() {
    return ratingMax;
  }

  public void setEnforceRating(Boolean enforceRating) {
    this.enforceRating.setValue(enforceRating);
  }

  public Boolean getEnforceRating() {
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
    if (simMods == null) {
      this.simMods.set(Map.of());
    } else {
      this.simMods.set(Collections.unmodifiableMap(simMods));
    }
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
    if (teams == null) {
      this.teams.set(Map.of());
    } else {
      this.teams.set(Collections.unmodifiableMap(teams));
    }
  }

  public ObjectProperty<Map<String, List<String>>> teamsProperty() {
    return teams;
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

  public Boolean isPasswordProtected() {
    return passwordProtected.get();
  }

  public BooleanProperty passwordProtectedProperty() {
    return passwordProtected;
  }

  public void setPasswordProtected(Boolean passwordProtected) {
    this.passwordProtected.setValue(passwordProtected);
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

  public OffsetDateTime getStartTime() {
    return startTime.get();
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime.set(startTime);
  }

  public ObjectProperty<OffsetDateTime> startTimeProperty() {
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
