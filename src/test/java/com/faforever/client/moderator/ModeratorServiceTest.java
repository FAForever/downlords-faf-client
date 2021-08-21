package com.faforever.client.moderator;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.UserService;
import com.faforever.commons.api.dto.MeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ModeratorServiceTest extends ServiceTest {

  @Mock
  private UserService userService;
  @Mock
  private FafServerAccessor fafServerAccessor;

  private ModeratorService instance;
  @BeforeEach
  public void setUp() throws Exception {
    instance = new ModeratorService(fafServerAccessor, userService);
  }

  @Test
  public void testGetPermissions() throws Exception {
    when(userService.getOwnUser()).thenReturn(new MeResult());
    instance.getPermissions();
    verify(userService).getOwnUser();
  }

  @Test
  public void testClosePlayersGame() throws Exception {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    instance.closePlayersGame(player);
    verify(fafServerAccessor).closePlayersGame(player.getId());
  }

  @Test
  public void testClosePlayersLobby() throws Exception {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    instance.closePlayersLobby(player);
    verify(fafServerAccessor).closePlayersLobby(player.getId());
  }

  @Test
  public void testBroadcast() throws Exception {
    instance.broadcastMessage("player");
    verify(fafServerAccessor).broadcastMessage("player");
  }
}
