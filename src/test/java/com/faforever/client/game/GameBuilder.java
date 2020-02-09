package com.faforever.client.game;

import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.VictoryCondition;
import javafx.collections.FXCollections;

import java.time.Instant;

public class GameBuilder {

  private final Game game;

  public GameBuilder() {
    game = new Game();
  }

  public static GameBuilder create() {
    return new GameBuilder();
  }

  public GameBuilder defaultValues() {
    game.setFeaturedMod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    game.setFeaturedModVersions(FXCollections.emptyObservableMap());
    game.setVictoryCondition(VictoryCondition.DEMORALIZATION);
    game.setHost("Host");
    game.setMapFolderName("mapName");
    game.setNumPlayers(1);
    game.setNumPlayers(2);
    game.setSimMods(FXCollections.emptyObservableMap());
    game.setStatus(GameStatus.OPEN);
    game.setTitle("Title");
    game.setTeams(FXCollections.emptyObservableMap());
    game.setId(1);
    game.setMaxRating(800);
    game.setMaxRating(1300);
    game.setStartTime(Instant.now());
    return this;
  }

  public Game get() {
    return game;
  }

  public GameBuilder title(String title) {
    game.setTitle(title);
    return this;
  }

  public GameBuilder featuredMod(String featuredMod) {
    game.setFeaturedMod(featuredMod);
    return this;
  }

  public GameBuilder state(GameStatus state) {
    game.setStatus(state);
    return this;
  }

  public GameBuilder password(String password) {
    game.setPassword(password);
    return this;
  }

  public GameBuilder host(String host) {
    game.setHost(host);
    return this;
  }
}
