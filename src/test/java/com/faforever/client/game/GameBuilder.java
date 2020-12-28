package com.faforever.client.game;

import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.GameType;
import com.faforever.client.remote.domain.VictoryCondition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.time.Instant;

public class GameBuilder {
  private final Game game = new Game();

  public static GameBuilder create() {
    return new GameBuilder();
  }

  public GameBuilder defaultValues() {
    passwordProtected(false);
    password("");
    featuredMod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    featuredModVersions(FXCollections.emptyObservableMap());
    victoryCondition(VictoryCondition.DEMORALIZATION);
    host("Host");
    mapFolderName("mapName");
    numPlayers(2);
    simMods(FXCollections.emptyObservableMap());
    status(GameStatus.OPEN);
    title("Title");
    teams(FXCollections.emptyObservableMap());
    id(1);
    minRating(800);
    maxRating(1300);
    startTime(Instant.now());
    gameType(GameType.CUSTOM);
    ratingType("global");
    return this;
  }


  public GameBuilder host(String host) {
    game.setHost(host);
    return this;
  }

  public GameBuilder title(String title) {
    game.setTitle(title);
    return this;
  }

  public GameBuilder mapFolderName(String mapFolderName) {
    game.setMapFolderName(mapFolderName);
    return this;
  }

  public GameBuilder featuredMod(String featuredMod) {
    game.setFeaturedMod(featuredMod);
    return this;
  }

  public GameBuilder id(int id) {
    game.setId(id);
    return this;
  }

  public GameBuilder numPlayers(int numPlayers) {
    game.setNumPlayers(numPlayers);
    return this;
  }

  public GameBuilder maxPlayers(int maxPlayers) {
    game.setMaxPlayers(maxPlayers);
    return this;
  }

  public GameBuilder averageRating(double averageRating) {
    game.setAverageRating(averageRating);
    return this;
  }

  public GameBuilder ratingType(String ratingType) {
    game.setRatingType(ratingType);
    return this;
  }

  public GameBuilder minRating(Integer minRating) {
    game.setMinRating(minRating);
    return this;
  }

  public GameBuilder maxRating(Integer maxRating) {
    game.setMaxRating(maxRating);
    return this;
  }

  public GameBuilder passwordProtected(boolean passwordProtected) {
    game.setPasswordProtected(passwordProtected);
    return this;
  }

  public GameBuilder password(String password) {
    game.setPassword(password);
    return this;
  }

  public GameBuilder visibility(GameVisibility visibility) {
    game.setVisibility(visibility);
    return this;
  }

  public GameBuilder status(GameStatus status) {
    game.setStatus(status);
    return this;
  }

  public GameBuilder victoryCondition(VictoryCondition victoryCondition) {
    game.setVictoryCondition(victoryCondition);
    return this;
  }

  public GameBuilder startTime(Instant startTime) {
    game.setStartTime(startTime);
    return this;
  }

  public GameBuilder enforceRating(boolean enforceRating) {
    game.setEnforceRating(enforceRating);
    return this;
  }

  public GameBuilder gameType(GameType gameType) {
    game.setGameType(gameType);
    return this;
  }

  public GameBuilder simMods(ObservableMap simMods) {
    game.setSimMods(simMods);
    return this;
  }

  public GameBuilder teams(ObservableMap teams) {
    game.setTeams(teams);
    return this;
  }

  public GameBuilder featuredModVersions(ObservableMap featuredModVersions) {
    game.setFeaturedModVersions(featuredModVersions);
    return this;
  }

  public Game get() {
    return game;
  }
}
