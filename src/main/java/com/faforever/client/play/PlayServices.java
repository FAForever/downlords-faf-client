package com.faforever.client.play;

import com.google.api.services.games.model.AchievementDefinition;
import com.google.api.services.games.model.PlayerAchievement;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PlayServices {

  CompletableFuture<Void> authorize();

  void startBatchUpdate();

  void executeBatchUpdate() throws IOException;

  void customGamePlayed() throws IOException;

  void ranked1v1GamePlayed() throws IOException;

  void ranked1v1GameWon() throws IOException;

  void killedCommanders(int number, boolean survived) throws IOException;

  void acuDamageReceived(double damage, boolean survived) throws IOException;

  void airUnitStats(long built, long killed) throws IOException;

  void landUnitStats(long built, long killed) throws IOException;

  void navalUnitStats(long built, long killed) throws IOException;

  void engineerStats(long built, long killed) throws IOException;

  void techUnitsBuilt(int builtTech1Units, int builtTech2Units, int builtTech3Units, int builtExperimentals) throws IOException;

  void resetBatchUpdate();

  void topScoringPlayer(int totalPlayers) throws IOException;

  CompletableFuture<List<PlayerAchievement>> getAchievements(String username);

  CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions();

  void connectedToGoogle() throws IOException;
}
