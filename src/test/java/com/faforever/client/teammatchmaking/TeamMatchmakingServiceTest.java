package com.faforever.client.teammatchmaking;

import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.GameLaunchMessageTestBuilder;
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
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.LobbyMode;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchCancelledMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchFoundMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchmakerInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchmakerInfoMessage.MatchmakerQueue;
import com.faforever.client.remote.domain.inbound.faf.PartyInviteMessage;
import com.faforever.client.remote.domain.inbound.faf.PartyKickedMessage;
import com.faforever.client.remote.domain.inbound.faf.SearchInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.UpdatePartyMessage;
import com.faforever.client.teammatchmaking.MatchmakingQueue.MatchingStatus;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.Faction;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;

import java.util.ArrayList;
import java.util.Arrays;
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

public class TeamMatchmakingServiceTest extends ServiceTest {

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

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBuilder.create("junit").defaultValues().get();
    otherPlayer = PlayerBuilder.create("junit2").defaultValues().id(2).get();
    List<Player> playerList = new ArrayList<>();
    playerList.add(player);
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(otherPlayer));
    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player));
    ReadOnlyObjectProperty<ConnectionState> state = new SimpleObjectProperty<>();
    instance = new TeamMatchmakingService(playerService, notificationService, preferencesService,
        fafService, eventBus, i18n, taskScheduler, gameService);

    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(player);
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
    PartyInviteMessage message = new PartyInviteMessage(1);

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


    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @NotNull
  private List<UpdatePartyMessage.PartyMember> generatePartyMembers(List<Integer> idList) {
    List<UpdatePartyMessage.PartyMember> testMembers = FXCollections.observableArrayList();
    idList.forEach(id -> {
      UpdatePartyMessage.PartyMember member = new UpdatePartyMessage.PartyMember(id, List.of());
      testMembers.add(member);
    });

    return testMembers;
  }

  @Test
  public void testOnPartyInfoMessagePlayerNotInParty() {
    List<UpdatePartyMessage.PartyMember> testMembers = generatePartyMembers(List.of(2));
    UpdatePartyMessage message = new UpdatePartyMessage(player.getId(), testMembers);

    instance.onPartyInfo(message);


    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @Test
  public void testOnPartyInfoMessage() {
    List<UpdatePartyMessage.PartyMember> testMembers = generatePartyMembers(List.of(1, 2));
    UpdatePartyMessage message = new UpdatePartyMessage(2, testMembers);

    instance.onPartyInfo(message);


    assertThat(instance.getParty().getMembers().size(), is(2));
    assertThat(instance.getParty().getOwner(), is(otherPlayer));
  }

  @Test
  public void testOnSearchInfoMessage() {
    SearchInfoMessage message = new SearchInfoMessage("notExistingQueue", MatchmakingState.START);

    instance.onSearchInfoMessage(message);

    verify(gameService, never()).startSearchMatchmaker();
    verify(gameService).onMatchmakerSearchStopped();

    MatchmakingQueue testQueue = new MatchmakingQueue();
    testQueue.setTechnicalName("testQueue");
    testQueue.setJoined(false);
    instance.getMatchmakingQueues().add(testQueue);

    SearchInfoMessage message2 = new SearchInfoMessage("testQueue", MatchmakingState.START);

    instance.onSearchInfoMessage(message2);


    verify(gameService).startSearchMatchmaker();
    assertThat(testQueue.isJoined(), is(true));
  }

  @Test
  public void testOnMatchFoundMessage() {
    setTwoQueues();
    MatchFoundMessage message = new MatchFoundMessage("queue1");

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
    GameLaunchMessage message = GameLaunchMessageTestBuilder.create().defaultValues().initMode(LobbyMode.AUTO_LOBBY).get();

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
    ObservableList<MatchmakerQueue> queues = FXCollections.observableArrayList();
    queues.addAll(messageQueue1, messageQueue2);
    return new MatchmakerInfoMessage(queues);
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
