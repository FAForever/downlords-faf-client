package com.faforever.client.play;

import com.faforever.client.game.Faction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockPlayServices implements PlayServices {

  @Override
  public CompletableFuture<Void> authorize() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void startBatchUpdate() {

  }

  @Override
  public void executeBatchUpdate() {

  }

  @Override
  public void customGamePlayed() {

  }

  @Override
  public void ranked1v1GamePlayed() {

  }

  @Override
  public void ranked1v1GameWon() {

  }

  @Override
  public void killedCommanders(int count, boolean survived) {

  }

  @Override
  public void acuDamageReceived(double damage, boolean survived) {

  }

  @Override
  public void resetBatchUpdate() {

  }

  @Override
  public void topScoringPlayer(int totalPlayers) {

  }

  @Override
  public ObservableList<PlayerAchievement> getPlayerAchievements(String username) {
    return FXCollections.emptyObservableList();
  }

  @Override
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return CompletableFuture.completedFuture(FXCollections.<AchievementDefinition>emptyObservableList());
  }

  @Override
  public void wonWithinDuration(Duration duration) {

  }

  @Override
  public void factionPlayed(Faction faction, boolean survived) {

  }

  @Override
  public void unitStats(int airBuilt, int airKilled, int landBuilt, int landKilled, int navalBuilt, int navalKilled, int tech1Built, int tech1Killed, int tech2Built, int tech2Killed, int tech3Built, int tech3Killed, int experimentalsBuilt, int experimentalsKilled, int engineersBuilt, int engineersKilled, boolean survived) {

  }

  @Override
  public void timePlayed(Duration duration, boolean survived) {

  }

  @Override
  public void asfBuilt(int count) {

  }

  @Override
  public void builtTransports(int count) {

  }

  @Override
  public void builtParagons(int count, boolean survived) {

  }

  @Override
  public void builtYolonaOss(int count, boolean survived) {

  }

  @Override
  public void builtScathis(int count, boolean survived) {

  }

  @Override
  public void builtSalvations(int count, boolean survived) {

  }

  @Override
  public void builtMavors(int count, boolean survived) {

  }

  @Override
  public void builtAtlantis(int count) {

  }

  @Override
  public void builtTempests(int count) {

  }

  @Override
  public void builtCzars(int count) {

  }

  @Override
  public void builtAhwasshas(int count) {

  }

  @Override
  public void builtYthothas(int count) {

  }

  @Override
  public void builtFatboys(int count) {

  }

  @Override
  public void builtMonkeylords(int count) {

  }

  @Override
  public void builtGalacticColossus(int count) {

  }

  @Override
  public void builtSoulRippers(int count) {

  }

  @Override
  public void builtMercies(int count) {

  }

  @Override
  public void builtFireBeetles(int count) {

  }

  @Override
  public void builtSupportCommanders(int count) {

  }

  @Override
  public void builtMegaliths(int count) {

  }

  @Override
  public void numberOfGamesPlayed(int numberOfGames) {

  }
}
