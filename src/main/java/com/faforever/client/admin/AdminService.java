package com.faforever.client.admin;

import com.faforever.client.remote.domain.PeriodType;

import java.util.concurrent.CompletableFuture;

public interface AdminService {

  CompletableFuture<Boolean> isAdmin();

  void banPlayer(int playerId, int duration, PeriodType periodType, String reason);

  void closePlayersGame(int playerId);

  void closePlayersLobby(int playerId);

  void broadcastMessage(String message);

}
