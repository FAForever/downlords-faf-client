package com.faforever.client.domain;

import com.faforever.commons.api.dto.VictoryCondition;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Value
public class GameBean {
  public static final int OBSERVERS_TEAM = -1;
  public static final int NO_TEAM = 1;

  StringProperty host = new SimpleStringProperty();
  @ToString.Include
  StringProperty title = new SimpleStringProperty();
  StringProperty mapFolderName = new SimpleStringProperty();
  StringProperty featuredMod = new SimpleStringProperty();
  @EqualsAndHashCode.Include
  @ToString.Include
  IntegerProperty id = new SimpleIntegerProperty();
  IntegerProperty maxPlayers = new SimpleIntegerProperty();
  StringProperty leaderboard = new SimpleStringProperty();
  ObjectProperty<Integer> ratingMin = new SimpleObjectProperty<>();
  ObjectProperty<Integer> ratingMax = new SimpleObjectProperty<>();
  BooleanProperty passwordProtected = new SimpleBooleanProperty();
  StringProperty password = new SimpleStringProperty();
  @ToString.Include
  ObjectProperty<GameStatus> status = new SimpleObjectProperty<>();
  ObjectProperty<VictoryCondition> victoryCondition = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> startTime = new SimpleObjectProperty<>();
  BooleanProperty enforceRating = new SimpleBooleanProperty();
  ObjectProperty<GameType> gameType = new SimpleObjectProperty<>();
  /**
   * Maps a sim mod's UID to its name.
   */
  ReadOnlyMapWrapper<String, String> simMods = new ReadOnlyMapWrapper<>(FXCollections.emptyObservableMap());
  ReadOnlyMapWrapper<Integer, List<Integer>> teams = new ReadOnlyMapWrapper<>(FXCollections.emptyObservableMap());
  ObservableValue<Set<Integer>> allPlayersInGame = teams.map(team -> team.values()
      .stream()
      .flatMap(Collection::stream)
      .collect(Collectors.toSet()))
      .orElse(Collections.emptySet());

  @Getter(AccessLevel.NONE)
  ObservableValue<Set<Integer>> activePlayersInGame = teams.map(team -> team.entrySet()
      .stream()
      .filter(entry -> OBSERVERS_TEAM != entry.getKey())
      .map(Entry::getValue)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet()))
      .orElse(Collections.emptySet());

  IntegerExpression numActivePlayers = IntegerBinding.integerExpression(activePlayersInGame.map(Collection::size).orElse(0));

  Set<ChangeListener<Set<Integer>>> playerChangeListeners = new HashSet<>();

  ChangeListener<Set<PlayerBean>> playerChangeListener = (observable, oldValue, newValue) -> {
    Set<PlayerBean> playersToRemove = oldValue.stream().filter(player -> !newValue.contains(player)).collect(Collectors.toSet());
    Set<PlayerBean> playersToAdd = newValue.stream().filter(player -> !oldValue.contains(player)).collect(Collectors.toSet());
    playersToRemove.stream().filter(player -> equals(player.getGame())).forEach(player -> player.setGame(null));
    playersToAdd.forEach(player -> player.setGame(this));
  };

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

  public int getMaxPlayers() {
    return maxPlayers.get();
  }

  public void setMaxPlayers(Integer maxPlayers) {
    this.maxPlayers.setValue(maxPlayers);
  }

  public IntegerProperty maxPlayersProperty() {
    return maxPlayers;
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
    if (this.simMods.equals(simMods)) {
      return;
    }

    this.simMods.set(simMods == null ? FXCollections.emptyObservableMap() : FXCollections.unmodifiableObservableMap(FXCollections.observableMap(simMods)));
  }

  public ReadOnlyMapProperty<String, String> simModsProperty() {
    return simMods.getReadOnlyProperty();
  }

  /**
   * Returns an unmodifiable map that maps team numbers (1, 2, ...) to a list of player ids.
   */
  public Map<Integer, List<Integer>> getTeams() {
    return teams.get();
  }

  public void setTeams(Map<Integer, List<Integer>> teams) {
    if (this.teams.equals(teams)) {
      return;
    }

    this.teams.set(teams == null ? FXCollections.emptyObservableMap() : FXCollections.unmodifiableObservableMap(FXCollections.observableMap(teams)));
  }

  public ReadOnlyMapProperty<Integer, List<Integer>> teamsProperty() {
    return teams.getReadOnlyProperty();
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

  public Collection<Integer> getAllPlayersInGame() {
    return allPlayersInGame.getValue();
  }

  public ObservableValue<Set<Integer>> allPlayersInGameProperty() {
    return allPlayersInGame;
  }

  public Set<Integer> getActivePlayersInGame() {
    return activePlayersInGame.getValue();
  }

  public ObservableValue<Set<Integer>> activePlayersInGameProperty() {
    return activePlayersInGame;
  }

  public int getNumActivePlayers() {
    return numActivePlayers.get();
  }

  public ObservableIntegerValue numActivePlayersProperty() {
    return numActivePlayers;
  }

  public void addPlayerChangeListener(ChangeListener<Set<Integer>> listener) {
    playerChangeListeners.add(listener);
    allPlayersInGame.addListener(listener);
  }

  public void removeListeners() {
    playerChangeListeners.forEach(allPlayersInGame::removeListener);
  }
}
