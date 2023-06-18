package com.faforever.client.moderator;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ModeratorService {
  private final FafServerAccessor fafServerAccessor;
  private final LoginService loginService;

  public Set<String> getPermissions() {
    return loginService.getOwnUser().getPermissions();
  }

  public void closePlayersGame(PlayerBean player) {
    fafServerAccessor.closePlayersGame(player.getId());
  }

  public void closePlayersLobby(PlayerBean player) {
    fafServerAccessor.closePlayersLobby(player.getId());
  }

  public void broadcastMessage(String message) {
    fafServerAccessor.broadcastMessage(message);
  }

}
