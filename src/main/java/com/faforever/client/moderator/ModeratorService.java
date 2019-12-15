package com.faforever.client.moderator;

import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ModeratorService {
  private final FafService fafService;
  private Set<String> cachedPermissions;

  public CompletableFuture<Set<String>> getPermissions() {
    if (cachedPermissions != null) {
      return CompletableFuture.completedFuture(cachedPermissions);
    }
    return fafService.getPermissions()
        .thenApply(permissions -> {
          cachedPermissions = permissions;
          return permissions;
        });
  }

  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {
    fafService.banPlayer(playerId, duration, periodType, reason);
  }

  public void closePlayersGame(int playerId) {
    fafService.closePlayersGame(playerId);
  }

  public void closePlayersLobby(int playerId) {
    fafService.closePlayersLobby(playerId);
  }

  public void broadcastMessage(String message) {
    fafService.broadcastMessage(message);
  }

}
