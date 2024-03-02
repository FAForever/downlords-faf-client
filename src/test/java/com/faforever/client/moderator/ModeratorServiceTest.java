package com.faforever.client.moderator;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import com.faforever.commons.api.dto.MeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ModeratorServiceTest extends ServiceTest {

  @Mock
  private LoginService loginService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @InjectMocks
  private ModeratorService instance;

  @Test
  public void testGetPermissions() throws Exception {
    when(loginService.getOwnUser()).thenReturn(new MeResult());
    instance.getPermissions();
    verify(loginService).getOwnUser();
  }

  @Test
  public void testClosePlayersGame() throws Exception {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    instance.closePlayersGame(player);
    verify(fafServerAccessor).closePlayersGame(player.getId());
  }

  @Test
  public void testClosePlayersLobby() throws Exception {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();
    instance.closePlayersLobby(player);
    verify(fafServerAccessor).closePlayersLobby(player.getId());
  }

  @Test
  public void testBroadcast() throws Exception {
    instance.broadcastMessage("player");
    verify(fafServerAccessor).broadcastMessage("player");
  }
}
