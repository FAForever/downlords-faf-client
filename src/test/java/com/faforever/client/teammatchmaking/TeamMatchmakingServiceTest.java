package com.faforever.client.teammatchmaking;

import com.faforever.client.fa.relay.LobbyMode;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.rankedmatch.MatchmakerInfoMessage.MatchmakerQueue;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.MatchCancelledMessage;
import com.faforever.client.remote.domain.MatchFoundMessage;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PartyInfoMessage;
import com.faforever.client.remote.domain.PartyInviteMessage;
import com.faforever.client.remote.domain.PartyKickedMessage;
import com.faforever.client.remote.domain.SearchInfoMessage;
import com.faforever.client.teammatchmaking.MatchmakingQueue.MatchingStatus;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.commons.api.dto.Faction;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamMatchmakingServiceTest extends AbstractPlainJavaFxTest {

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

  private Player player;
  private Player otherPlayer;
  private TeamMatchmakingService instance;

  @Before
  public void setUp() throws Exception {
    player = PlayerBuilder.create("junit").defaultValues().get();
    otherPlayer = PlayerBuilder.create("junit2").defaultValues().id(2).get();
    List<Player> playerList = new ArrayList<>();
    playerList.add(player);
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(otherPlayer));
    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player));
    ReadOnlyObjectProperty<ConnectionState> state = new SimpleObjectProperty<>();
    when(fafService.connectionStateProperty()).thenReturn(state);
    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<Player>(player));
    instance = new TeamMatchmakingService(playerService, notificationService, preferencesService,
        fafService, eventBus, i18n, taskScheduler, gameService);

    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
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
    PartyInviteMessage message = new PartyInviteMessage();
    message.setSender(1);

    instance.onPartyInvite(message);

    ArgumentCaptor<TransientNotification> captorTransient = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captorTransient.capture());
    ArgumentCaptor<PersistentNotification> captorPersistent = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(captorPersistent.capture());
    PersistentNotification persistentNotification = captorPersistent.getValue();
    assertThat(persistentNotification.getSeverity(), is(INFO));
    verify(i18n, times(2)).get("teammatchmaking.notification.invite.message", player.getUsername());
  }

  @Test
  public void testOnKickedFromPartyMessage() {
    setPartyMembers();
    setOwnerByName("member2");

    instance.onPartyKicked(new PartyKickedMessage());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
    assertThat(instance.partyMembersNotReady(), is(false));
  }

  @Test
  public void testOnKickedFromPartyMessageWhenInGame() {
    setPartyMembers();
    setOwnerByName("member2");
    player.setGame(GameBuilder.create().defaultValues().get());

    instance.onPartyKicked(new PartyKickedMessage());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @NotNull
  private List<PartyInfoMessage.PartyMember> generatePartyMembers(List<Integer> idList) {
    List<PartyInfoMessage.PartyMember> testMembers = FXCollections.observableArrayList();
    idList.forEach(id -> {
      PartyInfoMessage.PartyMember member = new PartyInfoMessage.PartyMember();
      member.setPlayer(id);
      member.setFactions(Collections.emptyList());
      testMembers.add(member);
    });

    return testMembers;
  }

  @Test
  public void testOnPartyInfoMessagePlayerNotInParty() {
    List<PartyInfoMessage.PartyMember> testMembers = generatePartyMembers(List.of(2));
    PartyInfoMessage message = new PartyInfoMessage();
    message.setMembers(testMembers);

    instance.onPartyInfo(message);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @Test
  public void testOnPartyInfoMessage() {
    List<PartyInfoMessage.PartyMember> testMembers = generatePartyMembers(List.of(1, 2));
    PartyInfoMessage message = new PartyInfoMessage();
    message.setMembers(testMembers);
    message.setOwner(2);

    instance.onPartyInfo(message);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.getParty().getMembers().size(), is(2));
    assertThat(instance.getParty().getOwner(), is(otherPlayer));
  }

  @Test
  public void testOnSearchInfoMessage() {
    SearchInfoMessage message = new SearchInfoMessage();
    message.setQueueName("notExistingQueue");
    message.setState(MatchmakingState.START);

    instance.onSearchInfoMessage(message);

    verify(gameService, never()).startSearchMatchmaker();
    verify(gameService).onMatchmakerSearchStopped();

    MatchmakingQueue testQueue = new MatchmakingQueue();
    testQueue.setTechnicalName("testQueue");
    testQueue.setJoined(false);
    instance.getMatchmakingQueues().add(testQueue);

    SearchInfoMessage message2 = new SearchInfoMessage();
    message2.setQueueName("testQueue");
    message2.setState(MatchmakingState.START);

    instance.onSearchInfoMessage(message2);
    WaitForAsyncUtils.waitForFxEvents();

    verify(gameService).startSearchMatchmaker();
    assertThat(testQueue.isJoined(), is(true));
  }

  @Test
  public void testOnMatchFoundMessage() {
    setTwoQueues();
    MatchFoundMessage message = new MatchFoundMessage();
    message.setQueueName("queue1");

    instance.onMatchFoundMessage(message);

    ArgumentCaptor<TransientNotification> captor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captor.capture());
    assertThat(instance.getMatchmakingQueues().get(0).getMatchingStatus(), is(MatchingStatus.MATCH_FOUND));
    assertThat(instance.getMatchmakingQueues().get(1).getMatchingStatus(), is(nullValue()));
    assertThat(instance.isCurrentlyInQueue(), is(false));
  }

  private void setTwoQueues() {
    instance.getMatchmakingQueues().clear();
    MatchmakingQueue queue1 = new MatchmakingQueue();
    queue1.setJoined(true);
    queue1.setTechnicalName("queue1");
    MatchmakingQueue queue2 = new MatchmakingQueue();
    queue2.setJoined(true);
    queue2.setTechnicalName("queue2");
    instance.getMatchmakingQueues().addAll(queue1, queue2);
  }

  @Test
  public void testOnMatchCancelledMessage() {
    testOnMatchFoundMessage();

    instance.onMatchCancelledMessage(new MatchCancelledMessage());

    assertThat(instance.getMatchmakingQueues().get(0).getMatchingStatus(), is(MatchingStatus.MATCH_CANCELLED));
    assertThat(instance.getMatchmakingQueues().get(1).getMatchingStatus(), is(nullValue()));
    verify(gameService).onMatchmakerSearchStopped();
  }

  @Test
  public void testOnGameLaunchMessage() {
    testOnMatchFoundMessage();
    GameLaunchMessage message = new GameLaunchMessage();
    message.setInitMode(LobbyMode.AUTO_LOBBY);

    instance.onGameLaunchMessage(message);

    assertThat(instance.getMatchmakingQueues().get(0).getMatchingStatus(), is(MatchingStatus.GAME_LAUNCHING));
    assertThat(instance.getMatchmakingQueues().get(1).getMatchingStatus(), is(nullValue()));
    verify(gameService).onMatchmakerSearchStopped();
  }

  @Test
  public void testOnMatchmakerInfoMessage() {
    instance.getMatchmakingQueues().clear();
    MatchmakingQueue queue1 = new MatchmakingQueue();
    queue1.setTechnicalName("queue1");
    instance.getMatchmakingQueues().add(queue1);
    when(fafService.getMatchmakingQueue("queue2")).thenReturn(CompletableFuture.completedFuture(Optional.of(new MatchmakingQueue())));

    instance.onMatchmakerInfo(createMatchmakerInfoMessage());

    verify(fafService, never()).getMatchmakingQueue("queue1");
    verify(fafService).getMatchmakingQueue("queue2");
    assertThat(instance.getMatchmakingQueues().size(), is(2));
  }

  @NotNull
  private MatchmakerInfoMessage createMatchmakerInfoMessage() {
    MatchmakerQueue messageQueue1 = new MatchmakerQueue("queue1", "2007-12-03T10:15:30+01:00",
        1, 0, List.of(), List.of());
    MatchmakerQueue messageQueue2 = new MatchmakerQueue("queue2", "2007-12-03T10:15:30+01:00",
        1, 0, List.of(), List.of());
    MatchmakerInfoMessage message = new MatchmakerInfoMessage();
    ObservableList<MatchmakerQueue> queues = FXCollections.observableArrayList();
    queues.addAll(messageQueue1, messageQueue2);
    message.setQueues(queues);
    return message;
  }

  @Test
  public void testInvitePlayer() {
    when(playerService.getPlayerByNameIfOnline("invitee")).thenReturn(Optional.of(otherPlayer));
    instance.currentlyInQueueProperty().set(false);

    instance.invitePlayer("invitee");

    verify(fafService).inviteToParty(otherPlayer);


    instance.currentlyInQueueProperty().set(true);

    instance.invitePlayer("invitee");

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());
  }

  @Test
  public void testAcceptInvite() {
    instance.currentlyInQueueProperty().set(false);

    instance.acceptPartyInvite(player);

    verify(fafService).acceptPartyInvite(player);
    verify(eventBus).post(new OpenTeamMatchmakingEvent());


    instance.currentlyInQueueProperty().set(true);

    instance.acceptPartyInvite(player);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());
    ImmediateNotification notification = captor.getValue();
    assertThat(notification.getSeverity(), is(WARN));
  }

  @Test
  public void testKickPlayer() {
    instance.kickPlayerFromParty(otherPlayer);

    verify(fafService).kickPlayerFromParty(otherPlayer);
  }

  @Test
  public void testLeaveParty() {

    instance.leaveParty();
    WaitForAsyncUtils.waitForFxEvents();

    verify(fafService).leaveParty();
    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
    assertThat(instance.partyMembersNotReady(), is(false));
  }

  @Test
  public void testSetPartyFactions() {
    List<Faction> factions = Arrays.asList(Faction.UEF, Faction.AEON);

    instance.sendFactionSelection(factions);

    verify(fafService).setPartyFactions(factions);
  }

  @Test
  public void testJoinQueue() {
    MatchmakingQueue queue = new MatchmakingQueue();

    Boolean success = instance.joinQueue(queue);

    verify(fafService).updateMatchmakerState(queue, MatchmakingState.START);
    assertThat(success, is(true));
  }

  @Test
  public void testJoinQueueWhileGameRunning() {
    when(gameService.isGameRunning()).thenReturn(true);
    MatchmakingQueue queue = new MatchmakingQueue();

    Boolean success = instance.joinQueue(queue);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());
    assertThat(success, is(false));
  }

  @Test
  public void testLeaveQueue() {
    MatchmakingQueue queue = new MatchmakingQueue();

    instance.leaveQueue(queue);

    verify(fafService).updateMatchmakerState(queue, MatchmakingState.STOP);
  }
}
