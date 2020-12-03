package com.faforever.client.teammatchmaking;

import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PartyInfoMessage;
import com.faforever.client.remote.domain.PartyInviteMessage;
import com.faforever.client.remote.domain.PartyKickedMessage;
import com.faforever.client.remote.domain.SearchInfoMessage;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TeamMatchmakingServiceTest extends AbstractPlainJavaFxTest {

  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private PlayerService playerService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafService fafService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private GameService gameService;
  @Mock
  private Player player;
  @Mock
  private Player otherPlayer;

  private TeamMatchmakingService instance;

  @Before
  public void setUp() throws Exception {
    List<Player> playerList = new ArrayList<>();
    playerList.add(player);
    when(playerService.getPlayersByIds(Collections.singletonList(1))).thenReturn(CompletableFuture.completedFuture(playerList));
    ReadOnlyObjectProperty<ConnectionState> state = new SimpleObjectProperty<>();
    when(fafService.connectionStateProperty()).thenReturn(state);
    ReadOnlyObjectProperty<Player> playerProperty = new SimpleObjectProperty<>();
    when(playerService.currentPlayerProperty()).thenReturn(playerProperty);
    instance = new TeamMatchmakingService(fafServerAccessor, playerService, notificationService, preferencesService,
        fafService, eventBus, i18n, taskScheduler, gameService);
  }

  private void setPartyMembers() {
    ObservableList<PartyMember> testMembers = FXCollections.observableArrayList();
    testMembers.add(new PartyMember(new Player("member1")));
    testMembers.add(new PartyMember(new Player("member2")));
    testMembers.add(new PartyMember(new Player("member3")));
    testMembers.add(new PartyMember(player));
    instance.getParty().setMembers(testMembers);
    instance.getParty().setOwner(player);
  }

  private void setOwnerByName(String owner) {
    instance.getParty().getMembers().stream()
        .filter(member -> member.getPlayer().getUsername().equals(owner))
        .findFirst()
        .ifPresent(partyMember1 -> instance.getParty().setOwner(partyMember1.getPlayer()));
  }

  @Test
  public void testOnInviteMessage() {
    when(player.getUsername()).thenReturn("dummy");
    PartyInviteMessage message = new PartyInviteMessage();
    message.setSender(1);

    instance.onPartyInvite(message);

    ArgumentCaptor<PersistentNotification> captorTransient = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(captorTransient.capture());
    ArgumentCaptor<PersistentNotification> captorPersistent = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(captorPersistent.capture());
    PersistentNotification persistentNotification = captorPersistent.getValue();
    assertThat(persistentNotification.getSeverity(), is(INFO));
    verify(i18n, times(2)).get("teammatchmaking.notification.invite.message", "dummy");
  }

  @Test
  public void testOnKickedFromPartyMessage() {
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    setPartyMembers();
    setOwnerByName("member2");

    instance.onPartyKicked(new PartyKickedMessage());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @Test
  public void testOnPartyInfoMessagePlayerNotInParty() {
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    List<PartyInfoMessage.PartyMember> testMembers = FXCollections.observableArrayList();
    PartyInfoMessage.PartyMember member = new PartyInfoMessage.PartyMember();
    member.setPlayer(2);
    testMembers.add(member);
    PartyInfoMessage message = new PartyInfoMessage();
    message.setMembers(testMembers);

    instance.onPartyInfo(message);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @Test
  public void testOnPartyInfoMessage() {

  }

  @Test
  public void testOnSearchInfoMessage() {
    SearchInfoMessage message = new SearchInfoMessage();
    message.setQueueName("notExistingQueue");
    message.setState(MatchmakingState.START);

    instance.onSearchInfoMessage(message);

    verifyNoInteractions(gameService);


    MatchmakingQueue testQueue = new MatchmakingQueue();
    testQueue.setQueueName("testQueue");
    testQueue.setJoined(false);
    instance.getMatchmakingQueues().add(testQueue);

    SearchInfoMessage message2 = new SearchInfoMessage();
    message2.setQueueName("testQueue");
    message2.setState(MatchmakingState.START);

    instance.onSearchInfoMessage(message2);

    verify(gameService).startSearchMatchmaker();
    assertThat(instance.getMatchmakingQueues().stream()
        .filter(queue -> queue.getQueueName().equals("testQueue")).findFirst().get().isJoined(), is(true));
  }

  @Test
  public void testOnMatchFoundMessage() {

  }

  @Test
  public void testOnMatchCancelledMessage() {

  }

  @Test
  public void testOnGameLaunchMessage() {

  }

  @Test
  public void testOnMatchmakerInfoMessage() {

  }

  @Test
  public void testInvitePlayer() {
    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(playerService.getPlayerForUsername("invitee")).thenReturn(Optional.of(otherPlayer));
    MatchmakingQueue queue = new MatchmakingQueue();
    queue.setJoined(false);
    instance.getMatchmakingQueues().add(queue);

    instance.invitePlayer("invitee");

    verify(fafServerAccessor).inviteToParty(otherPlayer);


    instance.getMatchmakingQueues().get(0).setJoined(true);

    instance.invitePlayer("invitee");

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());
    verifyNoMoreInteractions(fafServerAccessor);
  }

  @Test
  public void testAcceptInvite() {

  }

  @Test
  public void testKickPlayer() {

  }

  @Test
  public void testLeaveParty() {

  }
}
