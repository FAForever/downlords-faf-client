package com.faforever.client.builders;

import com.faforever.client.domain.GameBean;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.commons.api.dto.VictoryCondition;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GameVisibility;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;


public class GameBeanBuilder {
  public static GameBeanBuilder create() {
    return new GameBeanBuilder();
  }

  private final GameBean gameBean = new GameBean();

  public GameBeanBuilder defaultValues() {
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
    ratingMax(800);
    ratingMin(1300);
    startTime(OffsetDateTime.now());
    gameType(GameType.CUSTOM);
    leaderboard("global");
    return this;
  }

  public GameBeanBuilder host(String host) {
    gameBean.setHost(host);
    return this;
  }

  public GameBeanBuilder title(String title) {
    gameBean.setTitle(title);
    return this;
  }

  public GameBeanBuilder mapFolderName(String mapFolderName) {
    gameBean.setMapFolderName(mapFolderName);
    return this;
  }

  public GameBeanBuilder featuredMod(String featuredMod) {
    gameBean.setFeaturedMod(featuredMod);
    return this;
  }

  public GameBeanBuilder id(Integer id) {
    gameBean.setId(id);
    return this;
  }

  public GameBeanBuilder numPlayers(Integer numPlayers) {
    gameBean.setNumPlayers(numPlayers);
    return this;
  }

  public GameBeanBuilder maxPlayers(Integer maxPlayers) {
    gameBean.setMaxPlayers(maxPlayers);
    return this;
  }

  public GameBeanBuilder averageRating(double averageRating) {
    gameBean.setAverageRating(averageRating);
    return this;
  }

  public GameBeanBuilder leaderboard(String leaderboard) {
    gameBean.setLeaderboard(leaderboard);
    return this;
  }

  public GameBeanBuilder ratingMin(Integer ratingMin) {
    gameBean.setRatingMin(ratingMin);
    return this;
  }

  public GameBeanBuilder ratingMax(Integer ratingMax) {
    gameBean.setRatingMax(ratingMax);
    return this;
  }

  public GameBeanBuilder passwordProtected(boolean passwordProtected) {
    gameBean.setPasswordProtected(passwordProtected);
    return this;
  }

  public GameBeanBuilder password(String password) {
    gameBean.setPassword(password);
    return this;
  }

  public GameBeanBuilder visibility(GameVisibility visibility) {
    gameBean.setVisibility(visibility);
    return this;
  }

  public GameBeanBuilder status(GameStatus status) {
    gameBean.setStatus(status);
    return this;
  }

  public GameBeanBuilder victoryCondition(VictoryCondition victoryCondition) {
    gameBean.setVictoryCondition(victoryCondition);
    return this;
  }

  public GameBeanBuilder startTime(OffsetDateTime startTime) {
    gameBean.setStartTime(startTime);
    return this;
  }

  public GameBeanBuilder enforceRating(boolean enforceRating) {
    gameBean.setEnforceRating(enforceRating);
    return this;
  }

  public GameBeanBuilder gameType(GameType gameType) {
    gameBean.setGameType(gameType);
    return this;
  }

  public GameBeanBuilder simMods(Map<String, String> simMods) {
    gameBean.setSimMods(simMods);
    return this;
  }

  public GameBeanBuilder teams(Map<String, List<String>> teams) {
    gameBean.setTeams(teams);
    return this;
  }

  public GameBeanBuilder featuredModVersions(Map<String, Integer> featuredModVersions) {
    gameBean.setFeaturedModVersions(featuredModVersions);
    return this;
  }

  public GameBeanBuilder gameStatusListener(InvalidationListener gameStatusListener) {
    gameBean.setGameStatusListener(gameStatusListener);
    return this;
  }

  public GameBeanBuilder hostListener(InvalidationListener hostListener) {
    gameBean.setHostListener(hostListener);
    return this;
  }

  public GameBeanBuilder teamsListener(InvalidationListener teamsListener) {
    gameBean.setTeamsListener(teamsListener);
    return this;
  }

  public GameBean get() {
    return gameBean;
  }

}

