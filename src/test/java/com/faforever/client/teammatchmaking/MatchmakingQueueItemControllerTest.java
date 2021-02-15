package com.faforever.client.teammatchmaking;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.MatchmakingQueue.MatchingStatus;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.event.ActionEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchmakingQueueItemControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private EventBus eventBus;

  private Player player;
  private MatchmakingQueueItemController instance;
  private MatchmakingQueue queue;
  private Party party;
  private BooleanProperty partyMembersNotReadyProperty;

  @Before
  public void setUp() throws Exception {
    partyMembersNotReadyProperty = new ReadOnlyBooleanWrapper();

    queue = MatchmakingQueueBuilder.create().defaultValues().get();
    party = PartyBuilder.create().defaultValues().get();
    player = party.getOwner();
    when(teamMatchmakingService.getParty()).thenReturn(party);
    when(i18n.getOrDefault(eq(queue.getTechnicalName()), anyString())).thenReturn(queue.getTechnicalName());
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue())).thenReturn(String.valueOf(queue.getPlayersInQueue()));
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));

    instance = new MatchmakingQueueItemController(playerService, teamMatchmakingService, i18n, eventBus);
    when(teamMatchmakingService.partyMembersNotReadyProperty()).thenReturn(partyMembersNotReadyProperty);
    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(partyMembersNotReadyProperty.get());
    loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> instance.setQueue(queue));
  }

  @Test
  public void testQueueNameSet() {
    assertThat(instance.joinLeaveQueueButton.getText(), is(queue.getTechnicalName()));
  }

  @Test
  public void testShowMapPool() {
    instance.showMapPool(new ActionEvent());

    verify(eventBus).post(any(ShowMapPoolEvent.class));
  }

  @Test
  public void testOnJoinLeaveQueueButtonClicked() {
    when(teamMatchmakingService.joinQueue(any())).thenReturn(true);

    runOnFxThreadAndWait(() -> instance.joinLeaveQueueButton.fire());

    verify(teamMatchmakingService).joinQueue(queue);
    assertThat(instance.refreshingLabel.isVisible(), is(true));

    queue.setJoined(true);
    runOnFxThreadAndWait(() -> instance.joinLeaveQueueButton.fire());

    verify(teamMatchmakingService).leaveQueue(instance.queue);
    assertThat(instance.refreshingLabel.isVisible(), is(true));
  }

  @Test
  public void testOnJoinQueueFailed() {
    when(teamMatchmakingService.joinQueue(any())).thenReturn(false);

    runOnFxThreadAndWait(() -> instance.joinLeaveQueueButton.fire());

    assertThat(instance.joinLeaveQueueButton.isSelected(), is(false));
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
    runOnFxThreadAndWait(() -> queue.setPlayersInQueue(10));
    verify(i18n).get("teammatchmaking.playersInQueue", queue.getPlayersInQueue());
    assertThat(instance.playersInQueueLabel.getText(), is(String.valueOf(queue.getPlayersInQueue())));
  }

  @Test
  public void testQueueStateListener() {
    assertThat(instance.refreshingLabel.isVisible(), is(false));
    assertThat(instance.joinLeaveQueueButton.isSelected(), is(false));
    instance.refreshingLabel.setVisible(true);
    runOnFxThreadAndWait(() -> queue.setJoined(true));
    assertThat(instance.refreshingLabel.isVisible(), is(false));
    assertThat(instance.joinLeaveQueueButton.isSelected(), is(true));
    instance.refreshingLabel.setVisible(true);
    runOnFxThreadAndWait(() -> queue.setJoined(false));
    assertThat(instance.refreshingLabel.isVisible(), is(false));
    assertThat(instance.joinLeaveQueueButton.isSelected(), is(false));
  }

  @Test
  public void testPartySizeListener() {
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> party.getMembers().add(new PartyBuilder.PartyMemberBuilder(
        PlayerBuilder.create("notMe").defaultValues().get()).defaultValues().get()));
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> party.getMembers().setAll(party.getMembers().get(0)));
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(false));
  }

  @Test
  public void testTeamSizeListener() {
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> queue.setTeamSize(0));
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> queue.setTeamSize(2));
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(false));
  }

  @Test
  public void testPartyOwnerListener() {
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(false));

    runOnFxThreadAndWait(() -> party.setOwner(PlayerBuilder.create("notMe").defaultValues().id(100).get()));
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(true));

    runOnFxThreadAndWait(() -> party.setOwner(player));
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(false));
  }

  @Test
  public void testMembersNotReadyListener() {
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(false));

    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(true);
    runOnFxThreadAndWait(() -> partyMembersNotReadyProperty.set(true));
    assertThat(instance.joinLeaveQueueButton.isDisabled(), is(true));
  }
}
