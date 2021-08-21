package com.faforever.client.teammatchmaking;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.MatchmakerQueueBeanBuilder;
import com.faforever.client.builders.PartyBuilder.PartyMemberBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueBean.MatchingStatus;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.MatchmakerMapper;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.LobbyMode;
import com.faforever.commons.lobby.MatchmakerInfo;
import com.faforever.commons.lobby.MatchmakerMatchCancelledResponse;
import com.faforever.commons.lobby.MatchmakerMatchFoundResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.PartyInfo;
import com.faforever.commons.lobby.PartyInvite;
import com.faforever.commons.lobby.PartyKick;
import com.faforever.commons.lobby.SearchInfo;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.client.notification.Severity.WARN;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
  private FafServerAccessor fafServerAccessor;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private GameService gameService;
  @Mock
  private CompletableFuture<Void> matchmakingFuture;

  private PlayerBean player;
  private PlayerBean otherPlayer;
  private TeamMatchmakingService instance;
  private final MatchmakerMapper matchmakerMapper = Mappers.getMapper(MatchmakerMapper.class);


  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(matchmakerMapper);
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();
    otherPlayer = PlayerBeanBuilder.create().defaultValues().username("junit2").id(2).get();
    List<PlayerBean> playerList = new ArrayList<>();
    playerList.add(player);
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(otherPlayer));
    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player));
    when(gameService.startSearchMatchmaker()).thenReturn(matchmakingFuture);
    instance = new TeamMatchmakingService(playerService, notificationService, preferencesService,
        fafApiAccessor, fafServerAccessor, eventBus, i18n, taskScheduler, gameService, matchmakerMapper);

    when(preferencesService.isGamePathValid()).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(player);
  }

  private void setPartyMembers() {
    ObservableList<PartyMember> testMembers = FXCollections.observableArrayList();
    testMembers.add(PartyMemberBuilder.create(PlayerBeanBuilder.create().defaultValues().username("member1").get()).get());
    testMembers.add(PartyMemberBuilder.create(PlayerBeanBuilder.create().defaultValues().username("member2").get()).get());
    testMembers.add(PartyMemberBuilder.create(PlayerBeanBuilder.create().defaultValues().username("member3").get()).get());
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
    PartyInvite message = new PartyInvite(1);

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

    instance.onPartyKicked(new PartyKick());


    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
    assertThat(instance.partyMembersNotReady(), is(false));
  }

  @Test
  public void testOnKickedFromPartyMessageWhenInGame() {
    setPartyMembers();
    setOwnerByName("member2");
    player.setGame(GameBeanBuilder.create().defaultValues().get());

    instance.onPartyKicked(new PartyKick());


    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @NotNull
  private List<PartyInfo.PartyMember> generatePartyMembers(List<Integer> idList) {
    List<PartyInfo.PartyMember> testMembers = FXCollections.observableArrayList();
    idList.forEach(id -> {
      PartyInfo.PartyMember member = new PartyInfo.PartyMember(id, List.of());
      testMembers.add(member);
    });

    return testMembers;
  }

  @Test
  public void testOnPartyInfoMessagePlayerNotInParty() {
    List<PartyInfo.PartyMember> testMembers = generatePartyMembers(List.of(2));
    PartyInfo message = new PartyInfo(player.getId(), testMembers);

    instance.onPartyInfo(message);


    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
  }

  @Test
  public void testOnPartyInfoMessage() {
    List<PartyInfo.PartyMember> testMembers = generatePartyMembers(List.of(1, 2));
    PartyInfo message = new PartyInfo(2, testMembers);

    instance.onPartyInfo(message);


    assertThat(instance.getParty().getMembers().size(), is(2));
    assertThat(instance.getParty().getOwner(), is(otherPlayer));
  }

  @Test
  public void testOnSearchInfoMessage() {
    SearchInfo message = new SearchInfo("notExistingQueue", MatchmakerState.START);

    instance.onSearchInfoMessage(message);

    verify(gameService, never()).startSearchMatchmaker();

    MatchmakerQueueBean testQueue = new MatchmakerQueueBean();
    testQueue.setTechnicalName("testQueue");
    testQueue.setJoined(false);
    instance.getMatchmakerQueues().add(testQueue);

    SearchInfo message2 = new SearchInfo("testQueue", MatchmakerState.START);

    instance.onSearchInfoMessage(message2);


    verify(gameService).startSearchMatchmaker();
    assertThat(testQueue.isJoined(), is(true));
  }

  @Test
  public void testOnMatchFoundMessage() {
    setTwoQueues();
    MatchmakerMatchFoundResponse message = new MatchmakerMatchFoundResponse("queue1");

    instance.onMatchFoundMessage(message);

    ArgumentCaptor<TransientNotification> captor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captor.capture());
    assertThat(instance.getMatchmakerQueues().get(0).getMatchingStatus(), is(MatchingStatus.MATCH_FOUND));
    assertThat(instance.getMatchmakerQueues().get(1).getMatchingStatus(), is(nullValue()));
    assertThat(instance.isCurrentlyInQueue(), is(false));
  }

  private void setTwoQueues() {
    instance.getMatchmakerQueues().clear();
    MatchmakerQueueBean queue1 = new MatchmakerQueueBean();
    queue1.setJoined(true);
    queue1.setTechnicalName("queue1");
    MatchmakerQueueBean queue2 = new MatchmakerQueueBean();
    queue2.setJoined(true);
    queue2.setTechnicalName("queue2");
    instance.getMatchmakerQueues().addAll(queue1, queue2);
  }

  @Test
  public void testOnMatchCancelledMessage() {
    testOnMatchFoundMessage();

    instance.onMatchCancelledMessage(new MatchmakerMatchCancelledResponse());

    assertThat(instance.getMatchmakerQueues().get(0).getMatchingStatus(), is(MatchingStatus.MATCH_CANCELLED));
    assertThat(instance.getMatchmakerQueues().get(1).getMatchingStatus(), is(nullValue()));
  }

  @Test
  public void testOnGameLaunchMessage() {
    testOnMatchFoundMessage();
    GameLaunchResponse message = GameLaunchMessageBuilder.create().defaultValues().initMode(LobbyMode.AUTO_LOBBY).get();

    instance.onGameLaunchMessage(message);

    assertThat(instance.getMatchmakerQueues().get(0).getMatchingStatus(), is(MatchingStatus.GAME_LAUNCHING));
    assertThat(instance.getMatchmakerQueues().get(1).getMatchingStatus(), is(nullValue()));
  }

  @Test
  public void testOnMatchmakerInfoMessage() {
    instance.getMatchmakerQueues().clear();
    MatchmakerQueueBean queue1 = MatchmakerQueueBeanBuilder.create().defaultValues().get();
    queue1.setTechnicalName("queue1");
    instance.getMatchmakerQueues().add(queue1);
    MatchmakerQueue matchmakerQueue = new MatchmakerQueue().setLeaderboard(new Leaderboard().setId("1"));
    matchmakerQueue.setId("2");
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(matchmakerQueue));

    instance.onMatchmakerInfo(createMatchmakerInfoMessage());

    verify(fafApiAccessor, never()).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("technicalName").eq("queue1"))));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("technicalName").eq("queue2"))));
  }

  @NotNull
  private MatchmakerInfo createMatchmakerInfoMessage() {
    MatchmakerInfo.MatchmakerQueue messageQueue1 = new MatchmakerInfo.MatchmakerQueue("queue1", OffsetDateTime.ofInstant(Instant.now().plusSeconds(10), ZoneOffset.UTC),
        10, 1, 0, List.of(), List.of());
    MatchmakerInfo.MatchmakerQueue messageQueue2 = new MatchmakerInfo.MatchmakerQueue("queue2", OffsetDateTime.ofInstant(Instant.now().plusSeconds(10), ZoneOffset.UTC),
        10, 1, 0, List.of(), List.of());
    ObservableList<MatchmakerInfo.MatchmakerQueue> queues = FXCollections.observableArrayList();
    queues.addAll(messageQueue1, messageQueue2);
    return new MatchmakerInfo(queues);
  }

  @Test
  public void testInvitePlayer() {
    when(playerService.getPlayerByNameIfOnline("invitee")).thenReturn(Optional.of(otherPlayer));
    instance.currentlyInQueueProperty().set(false);

    instance.invitePlayer("invitee");

    verify(fafServerAccessor).inviteToParty(otherPlayer);


    instance.currentlyInQueueProperty().set(true);

    instance.invitePlayer("invitee");

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());
  }

  @Test
  public void testAcceptInvite() {
    instance.currentlyInQueueProperty().set(false);

    instance.acceptPartyInvite(player);

    verify(fafServerAccessor).acceptPartyInvite(player);
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

    verify(fafServerAccessor).kickPlayerFromParty(otherPlayer);
  }

  @Test
  public void testLeaveParty() {
    instance.leaveParty();

    verify(fafServerAccessor).leaveParty();
    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
    assertThat(instance.partyMembersNotReady(), is(false));
  }

  @Test
  public void testSetPartyFactions() {
    List<Faction> factions = List.of(Faction.UEF, Faction.AEON);

    instance.sendFactionSelection(factions);

    verify(fafServerAccessor).setPartyFactions(factions);
  }

  @Test
  public void testJoinQueue() {
    MatchmakerQueueBean queue = new MatchmakerQueueBean();

    Boolean success = instance.joinQueue(queue);

    verify(fafServerAccessor).gameMatchmaking(queue, MatchmakerState.START);
    assertThat(success, is(true));
  }

  @Test
  public void testJoinQueueWhileGameRunning() {
    when(gameService.isGameRunning()).thenReturn(true);
    MatchmakerQueueBean queue = new MatchmakerQueueBean();

    Boolean success = instance.joinQueue(queue);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());
    assertThat(success, is(false));
  }

  @Test
  public void testLeaveQueue() {
    MatchmakerQueueBean queue = new MatchmakerQueueBean();

    instance.leaveQueue(queue);

    verify(fafServerAccessor).gameMatchmaking(queue, MatchmakerState.STOP);
  }
}
