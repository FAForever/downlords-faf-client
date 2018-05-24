package com.faforever.client.moderator;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.dto.LegacyAccessLevel;
import com.faforever.client.api.dto.LobbyGroup;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.PeriodType;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ModeratorService {
  private static CompletableFuture isModerator;
  private final FafService fafService;
  private final FafApiAccessor fafApiAccessor;

  public ModeratorService(FafService fafService, FafApiAccessor fafApiAccessor) {
    this.fafService = fafService;
    this.fafApiAccessor = fafApiAccessor;
  }


  public CompletableFuture<Boolean> isModerator() {
    if (isModerator == null) {
      isModerator = CompletableFuture.supplyAsync(() -> {
        LegacyAccessLevel role = Optional.ofNullable(fafApiAccessor.getOwnPlayer().getLobbyGroup()).map(LobbyGroup::getAccessLevel).orElse(LegacyAccessLevel.ROLE_USER);
        return role == LegacyAccessLevel.ROLE_MODERATOR || role == LegacyAccessLevel.ROLE_ADMINISTRATOR;
      });
    }

    return isModerator;
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
