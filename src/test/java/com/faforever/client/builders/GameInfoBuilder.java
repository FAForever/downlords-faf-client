package com.faforever.client.builders;

import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.commons.api.dto.VictoryCondition;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import javafx.collections.FXCollections;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;


public class GameInfoBuilder {
  public static GameInfoBuilder create() {
    return new GameInfoBuilder();
  }

  private final GameInfo gameInfo = new GameInfo();

  public GameInfoBuilder defaultValues() {
    passwordProtected(false);
    featuredMod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    victoryCondition(VictoryCondition.DEMORALIZATION);
    host("Host");
    mapFolderName("mapName");
    simMods(FXCollections.emptyObservableMap());
    status(GameStatus.OPEN);
    title("Title");
    teams(FXCollections.emptyObservableMap());
    id(1);
    startTime(OffsetDateTime.now());
    gameType(GameType.CUSTOM);
    leaderboard("global");
    return this;
  }

  public GameInfoBuilder host(String host) {
    gameInfo.setHost(host);
    return this;
  }

  public GameInfoBuilder title(String title) {
    gameInfo.setTitle(title);
    return this;
  }

  public GameInfoBuilder mapFolderName(String mapFolderName) {
    gameInfo.setMapFolderName(mapFolderName);
    return this;
  }

  public GameInfoBuilder featuredMod(String featuredMod) {
    gameInfo.setFeaturedMod(featuredMod);
    return this;
  }

  public GameInfoBuilder id(Integer id) {
    gameInfo.setId(id);
    return this;
  }

  public GameInfoBuilder maxPlayers(Integer maxPlayers) {
    gameInfo.setMaxPlayers(maxPlayers);
    return this;
  }

  public GameInfoBuilder leaderboard(String leaderboard) {
    gameInfo.setLeaderboard(leaderboard);
    return this;
  }

  public GameInfoBuilder ratingMin(Integer ratingMin) {
    gameInfo.setRatingMin(ratingMin);
    return this;
  }

  public GameInfoBuilder ratingMax(Integer ratingMax) {
    gameInfo.setRatingMax(ratingMax);
    return this;
  }

  public GameInfoBuilder passwordProtected(boolean passwordProtected) {
    gameInfo.setPasswordProtected(passwordProtected);
    return this;
  }

  public GameInfoBuilder status(GameStatus status) {
    gameInfo.setStatus(status);
    return this;
  }

  public GameInfoBuilder victoryCondition(VictoryCondition victoryCondition) {
    gameInfo.setVictoryCondition(victoryCondition);
    return this;
  }

  public GameInfoBuilder startTime(OffsetDateTime startTime) {
    gameInfo.setStartTime(startTime);
    return this;
  }

  public GameInfoBuilder enforceRating(boolean enforceRating) {
    gameInfo.setEnforceRating(enforceRating);
    return this;
  }

  public GameInfoBuilder gameType(GameType gameType) {
    gameInfo.setGameType(gameType);
    return this;
  }

  public GameInfoBuilder simMods(Map<String, String> simMods) {
    gameInfo.setSimMods(simMods);
    return this;
  }

  public GameInfoBuilder teams(Map<Integer, List<Integer>> teams) {
    gameInfo.setTeams(FXCollections.unmodifiableObservableMap(FXCollections.observableMap(teams)));
    return this;
  }

  public GameInfo get() {
    return gameInfo;
  }

}

