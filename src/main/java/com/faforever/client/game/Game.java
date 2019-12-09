package com.faforever.client.game;

import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.VictoryCondition;
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

import java.time.Instant;
import java.util.List;

public class Game {
  private final StringProperty host;
  private final StringProperty title;
  private final StringProperty mapFolderName;
  private final StringProperty featuredMod;
  private final IntegerProperty id;
  private final IntegerProperty numPlayers;
  private final IntegerProperty maxPlayers;
  private final DoubleProperty averageRating;
  private final IntegerProperty minRating;
  private final IntegerProperty maxRating;
  private final BooleanProperty passwordProtected;
  private final StringProperty password;
  private final ObjectProperty<GameVisibility> visibility;
  private final ObjectProperty<GameStatus> status;
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

  public Game() {
    id = new SimpleIntegerProperty();
    host = new SimpleStringProperty();
    title = new SimpleStringProperty();
    mapFolderName = new SimpleStringProperty();
    featuredMod = new SimpleStringProperty();
    numPlayers = new SimpleIntegerProperty();
    maxPlayers = new SimpleIntegerProperty();
    averageRating = new SimpleDoubleProperty(0);
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
   * Maps team names ("1", "2", ...) to a list of player names. <strong>Make sure to synchronize on the return
   * value.</strong>
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

  @Override
  public String toString() {
    return "Game{" +
        "title=" + title.get() +
        ", id=" + id.get() +
        ", status=" + status.get() +
        '}';
  }
}
