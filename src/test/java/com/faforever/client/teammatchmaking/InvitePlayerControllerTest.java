package com.faforever.client.teammatchmaking;

import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

public class InvitePlayerControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private PlayerService playerService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private UiService uiService;

  private InvitePlayerController instance;

  @Before
  public void setUp() throws IOException {
    when(playerService.getPlayerNames())
        .thenReturn(Set.of("axel12", "TrustTheFall", "nInPrisonForWhat", "Sheikah"));
    when(playerService.getCurrentPlayer())
        .thenReturn(PlayerBuilder.create("axel12").defaultValues().get());
    instance = new InvitePlayerController(playerService, uiService, teamMatchmakingService);
    loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml", clazz -> instance);
  }

  @Test
  public void testGivenInviteUiWhenFilterIsFilledOutShouldShowUsersToInviteThatMatchFilter() {
    instance.playerTextField.setText("T");
    assertThat(instance.playersListView.getItems(), hasSize(2));
  }
}