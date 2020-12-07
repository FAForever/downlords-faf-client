package com.faforever.client.teammatchmaking;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
  @Mock
  private Player player;

  private MatchmakingQueueItemController instance;

  @Before
  public void setUp() throws Exception {
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.get("teammatchmaking.playersInQueue", 1)).thenReturn("");
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    prepareParty();

    instance = new MatchmakingQueueItemController(playerService, teamMatchmakingService, i18n, eventBus);
    loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml", clazz -> instance);
    MatchmakingQueue queue = new MatchmakingQueue();
    queue.setQueueName("1");
    queue.setPlayersInQueue(1);
    queue.setTeamSize(2);
    queue.setJoined(false);
    instance.setQueue(queue);
  }

  private void prepareParty() {
    Party party = new Party();
    party.setOwner(player);
    PartyMember member = mock(PartyMember.class);
    ObservableList<PartyMember> members = FXCollections.observableArrayList();
    members.add(member);
    party.setMembers(members);
    when(teamMatchmakingService.getParty()).thenReturn(party);
  }

  @Test
  public void testShowMapPool() {
    instance.showMapPool(new ActionEvent());

    ArgumentCaptor<ShowMapPoolEvent> captor = ArgumentCaptor.forClass(ShowMapPoolEvent.class);
    verify(eventBus).post(captor.capture());
  }

  @Test
  public void testOnJoinLeaveQueueButtonClicked() {
    when(teamMatchmakingService.joinQueue(any())).thenReturn(true);

    instance.joinLeaveQueueButton.fire();
    WaitForAsyncUtils.waitForFxEvents();

    verify(teamMatchmakingService).joinQueue(instance.queue);
    assertThat(instance.joinLeaveQueueButton.selectedProperty().get(), is(true));

    instance.queue.setJoined(true);
    instance.joinLeaveQueueButton.fire();
    WaitForAsyncUtils.waitForFxEvents();

    verify(teamMatchmakingService).leaveQueue(instance.queue);
    assertThat(instance.joinLeaveQueueButton.selectedProperty().get(), is(false));
  }

  @Test
  public void testOnJoinQueueFailed() {
    when(teamMatchmakingService.joinQueue(any())).thenReturn(false);

    instance.joinLeaveQueueButton.fire();
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.joinLeaveQueueButton.selectedProperty().get(), is(false));
  }
}
