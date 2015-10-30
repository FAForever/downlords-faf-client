package com.faforever.client.play;

import com.faforever.client.config.CacheNames;
import com.faforever.client.game.Faction;
import javafx.collections.ObservableList;
import org.springframework.cache.annotation.Cacheable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PlayServices {

  CompletableFuture<Void> authorize();

  void startBatchUpdate();

  void executeBatchUpdate();

  void customGamePlayed();

  void ranked1v1GamePlayed();

  void ranked1v1GameWon();

  void killedCommanders(int count, boolean survived);

  void acuDamageReceived(double damage, boolean survived);

  void resetBatchUpdate();

  void topScoringPlayer(int totalPlayers);

  ObservableList<PlayerAchievement> getPlayerAchievements(String username);

  @Cacheable(CacheNames.ACHIEVEMENTS)
  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  void wonWithinDuration(Duration duration);

  void factionPlayed(Faction faction, boolean survived);

  void unitStats(int airBuilt, int airKilled,
                 int landBuilt, int landKilled,
                 int navalBuilt, int navalKilled,
                 int tech1Built, int tech1Killed,
                 int tech2Built, int tech2Killed,
                 int tech3Built, int tech3Killed,
                 int experimentalsBuilt, int experimentalsKilled, int engineersBuilt, int engineersKilled, boolean survived);

  void timePlayed(Duration duration, boolean survived);

  void asfBuilt(int count);

  void builtTransports(int count);

  void builtParagons(int count, boolean survived);

  void builtYolonaOss(int count, boolean survived);

  void builtScathis(int count, boolean survived);

  void builtSalvations(int count, boolean survived);

  void builtMavors(int count, boolean survived);

  void builtAtlantis(int count);

  void builtTempests(int count);

  void builtCzars(int count);

  void builtAhwasshas(int count);

  void builtYthothas(int count);

  void builtFatboys(int count);

  void builtMonkeylords(int count);

  void builtGalacticColossus(int count);

  void builtSoulRippers(int count);

  void builtMercies(int count);

  void builtFireBeetles(int count);

  void builtSupportCommanders(int count);

  void builtMegaliths(int count);

  void numberOfGamesPlayed(int numberOfGames);
}
