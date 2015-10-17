package com.faforever.client.play;

import com.faforever.client.game.Faction;
import com.google.api.services.games.model.AchievementDefinition;
import com.google.api.services.games.model.PlayerAchievement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
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
  public void executeBatchUpdate() throws IOException {

  }

  @Override
  public void customGamePlayed() throws IOException {

  }

  @Override
  public void ranked1v1GamePlayed() throws IOException {

  }

  @Override
  public void ranked1v1GameWon() throws IOException {

  }

  @Override
  public void killedCommanders(int count, boolean survived) throws IOException {

  }

  @Override
  public void acuDamageReceived(double damage, boolean survived) throws IOException {

  }

  @Override
  public void resetBatchUpdate() {

  }

  @Override
  public void topScoringPlayer(int totalPlayers) throws IOException {

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
  public void playerRating1v1(int rating) throws IOException {

  }

  @Override
  public void playerRatingGlobal(int rating) throws IOException {

  }

  @Override
  public void wonWithinDuration(Duration duration) throws IOException {

  }

  @Override
  public void playedFaction(Faction faction, boolean survived) throws IOException {

  }

  @Override
  public void unitStats(int airBuilt, int airKilled, int landBuilt, int landKilled, int navalBuilt, int navalKilled, int tech1Built, int tech1Killed, int tech2Built, int tech2Killed, int tech3Built, int tech3Killed, int experimentalsBuilt, int experimentalsKilled, int engineersBuilt, int engineersKilled, boolean survived) throws IOException {

  }

  @Override
  public void timePlayed(Duration duration, boolean survived) throws IOException {

  }

  @Override
  public void asfBuilt(int count) throws IOException {

  }

  @Override
  public void builtTransports(int count) throws IOException {

  }

  @Override
  public void builtParagons(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtYolonaOss(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtScathis(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtSalvations(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtMavors(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtAtlantis(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtTempests(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtCzars(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtAhwasshas(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtYthothas(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtFatboys(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtMonkeylords(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtGalacticColossus(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtSoulRippers(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtMercies(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtFireBeetles(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtSupportCommanders(int count, boolean survived) throws IOException {

  }

  @Override
  public void builtMegaliths(int count, boolean survived) throws IOException {

  }
}
