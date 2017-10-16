package com.faforever.client.admin;

import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.PeriodType;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AdminServiceImpl implements AdminService {
  private static Boolean admin;
  private final FafService fafService;

  public AdminServiceImpl(FafService fafService) {
    this.fafService = fafService;
  }


  public CompletableFuture<Boolean> isAdmin() {
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {
    fafService.banPlayer(playerId, duration, periodType, reason);
  }

  @Override
  public void closePlayersGame(int playerId) {
    fafService.closePlayersGame(playerId);
  }

  @Override
  public void closePlayersLobby(int playerId) {
    fafService.closePlayersLobby(playerId);
  }

  @Override
  public void broadcastMessage(String message) {
    fafService.broadcastMessage(message);
  }
}
