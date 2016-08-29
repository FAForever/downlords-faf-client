package com.faforever.client.game;

import com.faforever.client.remote.domain.GameState;
import com.faforever.client.remote.domain.VictoryCondition;
import javafx.collections.FXCollections;

public class GameInfoBeanBuilder {

  private final Game game;

  public GameInfoBeanBuilder() {
    game = new Game();
  }

  public static GameInfoBeanBuilder create() {
    return new GameInfoBeanBuilder();
  }

  public GameInfoBeanBuilder defaultValues() {
    game.setFeaturedMod(KnownFeaturedMod.DEFAULT.getString());
    game.setFeaturedModVersions(FXCollections.emptyObservableMap());
    game.setVictoryCondition(VictoryCondition.DEMORALIZATION);
    game.setHost("Host");
    game.setMapFolderName("mapName");
    game.setNumPlayers(1);
    game.setNumPlayers(2);
    game.setSimMods(FXCollections.emptyObservableMap());
    game.setStatus(GameState.OPEN);
    game.setTitle("Title");
    game.setTeams(FXCollections.emptyObservableMap());
    game.setId(1);
    return this;
  }

  public Game get() {
    return game;
  }

  public GameInfoBeanBuilder title(String title) {
    game.setTitle(title);
    return this;
  }

  public GameInfoBeanBuilder featuredMod(String featuredMod) {
    game.setFeaturedMod(featuredMod);
    return this;
  }
}
