package com.faforever.client.teammatchmaking;

import com.faforever.client.builders.MatchmakerQueueInfoBuilder;
import com.faforever.client.builders.PartyInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.domain.server.PartyInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import com.faforever.commons.lobby.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchmakingQueueItemControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private LoginService loginService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private NavigationHandler navigationHandler;

  private PlayerInfo player;
  @InjectMocks
  private MatchmakingQueueItemController instance;
  private MatchmakerQueueInfo queue;
  private PartyInfo party;
  private BooleanProperty partyMembersNotReadyProperty;

  @BeforeEach
  public void setUp() throws Exception {
    partyMembersNotReadyProperty = new ReadOnlyBooleanWrapper();

    queue = MatchmakerQueueInfoBuilder.create().defaultValues().get();
    party = PartyInfoBuilder.create().defaultValues().get();
    player = party.getOwner();
    lenient().when(teamMatchmakingService.getParty()).thenReturn(party);
    lenient().when(i18n.getOrDefault(eq(queue.getTechnicalName()), anyString())).thenReturn(queue.getTechnicalName());
    lenient().when(i18n.get(anyString())).thenReturn("");
    lenient().when(i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue()))
             .thenReturn(String.valueOf(queue.getPlayersInQueue()));
    lenient().when(i18n.get("teammatchmaking.activeGames", queue.getActiveGames()))
             .thenReturn(String.valueOf(queue.getActiveGames()));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new ReadOnlyObjectWrapper<>(player));
    Player ownPlayer = new Player(0, "junit", null, null, "us", null, Map.of(), null);
    lenient().when(loginService.getOwnPlayer()).thenReturn(ownPlayer);
    lenient().when(loginService.ownPlayerProperty()).thenReturn(new SimpleObjectProperty<>(ownPlayer));
    lenient().when(loginService.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
    lenient().when(loginService.connectionStateProperty())
             .thenReturn(new SimpleObjectProperty<>(ConnectionState.CONNECTED));

    lenient().when(teamMatchmakingService.partyMembersNotReadyProperty()).thenReturn(partyMembersNotReadyProperty);
    lenient().when(teamMatchmakingService.partyMembersNotReady()).thenReturn(partyMembersNotReadyProperty.get());
    loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> {
      instance.setQueue(queue);
    });
  }

  @Test
  public void testQueueNameSet() {
    assertThat(instance.selectButton.getText(), is(queue.getTechnicalName()));
  }

  @Test
  public void testOnJoinLeaveQueueButtonClicked() {
    runOnFxThreadAndWait(() -> instance.selectButton.fire());

    assertThat(instance.getQueue().isSelected(), is(true));

    runOnFxThreadAndWait(() -> instance.selectButton.fire());

    assertThat(instance.getQueue().isSelected(), is(false));
  }

  @Test
  public void testMatchStatusListeners() {
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));

    queue.setMatchingStatus(MatchingStatus.MATCH_FOUND);
    assertThat(instance.matchFoundLabel.isVisible(), is(true));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));

    queue.setMatchingStatus(MatchingStatus.MATCH_CANCELLED);
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(true));

    queue.setMatchingStatus(MatchingStatus.GAME_LAUNCHING);
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(true));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));

    queue.setMatchingStatus(null);
    assertThat(instance.matchFoundLabel.isVisible(), is(false));
    assertThat(instance.matchStartingLabel.isVisible(), is(false));
    assertThat(instance.matchCancelledLabel.isVisible(), is(false));
  }

  @Test
  public void testPopulationListener() {
    assertThat(instance.playersInQueueLabel.getText(), is(String.valueOf(queue.getPlayersInQueue())));
    when(i18n.get(eq("teammatchmaking.playersInQueue"), anyInt())).thenReturn("10");
    queue.setPlayersInQueue(10);
    assertThat(instance.playersInQueueLabel.getText(), is(String.valueOf(queue.getPlayersInQueue())));
    verify(i18n).get("teammatchmaking.playersInQueue", queue.getPlayersInQueue());
  }

  @Test
  public void testActiveGamesListener() {
    assertThat(instance.activeGamesLabel.getText(), is(String.valueOf(queue.getActiveGames())));
    when(i18n.get(eq("teammatchmaking.activeGames"), anyInt())).thenReturn("10");
    runOnFxThreadAndWait(() -> queue.setActiveGames(10));
    assertThat(instance.activeGamesLabel.getText(), is(String.valueOf(queue.getActiveGames())));
    verify(i18n).get("teammatchmaking.activeGames", queue.getActiveGames());
  }

  @Test
  public void testPartySizeListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> party.getMembers()
                                    .add(new PartyInfoBuilder.PartyMemberBuilder(PlayerInfoBuilder.create()
                                                                                                  .defaultValues()
                                                                                                  .username("notMe")
                                                                                                  .get()).defaultValues()
                                                                                                         .get()));
    assertThat(instance.selectButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> party.getMembers().setAll(party.getMembers().getFirst()));
    assertThat(instance.selectButton.isDisabled(), is(false));
  }

  @Test
  public void testTeamSizeListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> queue.setTeamSize(0));
    assertThat(instance.selectButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> queue.setTeamSize(2));
    assertThat(instance.selectButton.isDisabled(), is(false));
  }

  @Test
  public void testPartyOwnerListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    runOnFxThreadAndWait(
        () -> party.setOwner(PlayerInfoBuilder.create().defaultValues().username("notMe").id(100).get()));
    assertThat(instance.selectButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> party.setOwner(player));
    assertThat(instance.selectButton.isDisabled(), is(false));
  }

  @Test
  public void testMembersNotReadyListener() {
    assertThat(instance.selectButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> partyMembersNotReadyProperty.set(true));
    assertThat(instance.selectButton.isDisabled(), is(true));
  }
}
