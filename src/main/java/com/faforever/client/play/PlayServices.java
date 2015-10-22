package com.faforever.client.play;

import com.faforever.client.config.CacheNames;
import com.faforever.client.game.Faction;
import javafx.collections.ObservableList;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PlayServices {

  CompletableFuture<Void> authorize();

  void startBatchUpdate();

  void executeBatchUpdate() throws IOException;

  void customGamePlayed() throws IOException;

  void ranked1v1GamePlayed() throws IOException;

  void ranked1v1GameWon() throws IOException;

  void killedCommanders(int count, boolean survived) throws IOException;

  void acuDamageReceived(double damage, boolean survived) throws IOException;

  void resetBatchUpdate();

  void topScoringPlayer(int totalPlayers) throws IOException;

  ObservableList<PlayerAchievement> getPlayerAchievements(String username);

  @Cacheable(CacheNames.ACHIEVEMENTS)
  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  void playerRating1v1(int rating) throws IOException;

  void playerRatingGlobal(int rating) throws IOException;

  void wonWithinDuration(Duration duration) throws IOException;

  void factionPlayed(Faction faction, boolean survived) throws IOException;

  void unitStats(int airBuilt, int airKilled,
                 int landBuilt, int landKilled,
                 int navalBuilt, int navalKilled,
                 int tech1Built, int tech1Killed,
                 int tech2Built, int tech2Killed,
                 int tech3Built, int tech3Killed,
                 int experimentalsBuilt, int experimentalsKilled, int engineersBuilt, int engineersKilled, boolean survived) throws IOException;

  void timePlayed(Duration duration, boolean survived) throws IOException;

  void asfBuilt(int count) throws IOException;

  void builtTransports(int count) throws IOException;

  void builtParagons(int count, boolean survived) throws IOException;

  void builtYolonaOss(int count, boolean survived) throws IOException;

  void builtScathis(int count, boolean survived) throws IOException;

  void builtSalvations(int count, boolean survived) throws IOException;

  void builtMavors(int count, boolean survived) throws IOException;

  void builtAtlantis(int count, boolean survived) throws IOException;

  void builtTempests(int count, boolean survived) throws IOException;

  void builtCzars(int count, boolean survived) throws IOException;

  void builtAhwasshas(int count, boolean survived) throws IOException;

  void builtYthothas(int count, boolean survived) throws IOException;

  void builtFatboys(int count, boolean survived) throws IOException;

  void builtMonkeylords(int count, boolean survived) throws IOException;

  void builtGalacticColossus(int count, boolean survived) throws IOException;

  void builtSoulRippers(int count, boolean survived) throws IOException;

  void builtMercies(int count, boolean survived) throws IOException;

  void builtFireBeetles(int count, boolean survived) throws IOException;

  void builtSupportCommanders(int count, boolean survived) throws IOException;

  void builtMegaliths(int count, boolean survived) throws IOException;

  void numberOfGamesPlayed(int numberOfGames) throws IOException;
}
