package com.faforever.client.moderator;

import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.user.UserService;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModeratorService {
  private final FafService fafService;
  private final UserService userService;

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

  public ObservableBooleanValue permissionsContainBinding(String permission) {
    return Bindings.createBooleanBinding(() -> userService.getOwnUser().getPermissions().contains(permission), userService.ownUserProperty());
  }
}
