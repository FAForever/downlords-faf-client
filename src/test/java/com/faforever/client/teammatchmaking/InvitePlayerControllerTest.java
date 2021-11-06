package com.faforever.client.teammatchmaking;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

public class InvitePlayerControllerTest extends UITest {

  @Mock
  private PlayerService playerService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private UiService uiService;

  private InvitePlayerController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(playerService.getPlayerNames())
        .thenReturn(Set.of("axel12", "TrustTheFall", "nInPrisonForWhat", "Sheikah"));
    when(playerService.getCurrentPlayer())
        .thenReturn(PlayerBeanBuilder.create().defaultValues().username("axel12").get());
    instance = new InvitePlayerController(playerService, uiService, teamMatchmakingService);
    loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml", clazz -> instance);
  }

  @Test
  public void testGivenInviteUiWhenFilterIsFilledOutShouldShowUsersToInviteThatMatchFilter() {
    instance.playerTextField.setText("T");
    assertThat(instance.playersListView.getItems(), hasSize(2));
  }
}