package com.faforever.client.teammatchmaking;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.PartyBuilder.PartyMemberBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MatchingStatus;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.MatchmakerMapper;
import com.faforever.client.mod.ModService;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.LobbyMode;
import com.faforever.commons.lobby.MatchmakerInfo;
import com.faforever.commons.lobby.MatchmakerMatchCancelledResponse;
import com.faforever.commons.lobby.MatchmakerMatchFoundResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.PartyInfo;
import com.faforever.commons.lobby.PartyInvite;
import com.faforever.commons.lobby.PartyKick;
import com.faforever.commons.lobby.SearchInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.notification.Severity.INFO;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamMatchmakingServiceTest extends ServiceTest {

  @Mock
  private MapService mapService;
  @Mock
  private ModService modService;
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
  private I18n i18n;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private LoginService loginService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Mock
  private GameService gameService;
  @Mock
  private NavigationHandler navigationHandler;
  @Spy
  private MatchmakerPrefs matchmakerPrefs = new MatchmakerPrefs();

  private PlayerBean player;
  private PlayerBean otherPlayer;
  @InjectMocks
  private TeamMatchmakingService instance;
  @Spy
  private final MatchmakerMapper matchmakerMapper = Mappers.getMapper(MatchmakerMapper.class);

  private final BooleanProperty loggedIn = new SimpleBooleanProperty();

  private final TestPublisher<MatchmakerInfo> matchmakerInfoTestPublisher = TestPublisher.create();
  private final TestPublisher<MatchmakerMatchFoundResponse> matchmakerFoundTestPublisher = TestPublisher.create();
  private final TestPublisher<MatchmakerMatchCancelledResponse> matchmakerCancelledTestPublisher = TestPublisher.create();
  private final TestPublisher<PartyInvite> inviteTestPublisher = TestPublisher.create();
  private final TestPublisher<PartyKick> kickTestPublisher = TestPublisher.create();
  private final TestPublisher<PartyInfo> partyInfoTestPublisher = TestPublisher.create();
  private final TestPublisher<SearchInfo> searchInfoTestPublisher = TestPublisher.create();
  private final TestPublisher<GameLaunchResponse> gameLaunchResponseTestPublisher = TestPublisher.create();
  private final SimpleObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(
      ConnectionState.DISCONNECTED);
  ;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(matchmakerMapper);
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();
    otherPlayer = PlayerBeanBuilder.create().defaultValues().username("junit2").id(2).get();
    when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(otherPlayer));
    when(playerService.getPlayerByIdIfOnline(1)).thenReturn(Optional.of(player));
    when(gameService.getGames()).thenReturn(FXCollections.emptyObservableList());
    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());

    when(loginService.loggedInProperty()).thenReturn(loggedIn);
    when(fafServerAccessor.getEvents(MatchmakerInfo.class)).thenReturn(matchmakerInfoTestPublisher.flux());
    when(fafServerAccessor.getEvents(MatchmakerMatchFoundResponse.class)).thenReturn(matchmakerFoundTestPublisher.flux());
    when(fafServerAccessor.getEvents(MatchmakerMatchCancelledResponse.class)).thenReturn(matchmakerCancelledTestPublisher.flux());
    when(fafServerAccessor.getEvents(PartyInvite.class)).thenReturn(inviteTestPublisher.flux());
    when(fafServerAccessor.getEvents(PartyKick.class)).thenReturn(kickTestPublisher.flux());
    when(fafServerAccessor.getEvents(PartyInfo.class)).thenReturn(partyInfoTestPublisher.flux());
    when(fafServerAccessor.getEvents(SearchInfo.class)).thenReturn(searchInfoTestPublisher.flux());
    when(fafServerAccessor.getEvents(GameLaunchResponse.class)).thenReturn(gameLaunchResponseTestPublisher.flux());
    when(fafServerAccessor.connectionStateProperty()).thenReturn(connectionState);

    when(preferencesService.isValidGamePath()).thenReturn(true);
    when(playerService.getCurrentPlayer()).thenReturn(player);
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());


    instance.getParty().setOwner(player);

    instance.afterPropertiesSet();
  }

  private void setPartyMembers() {
    ObservableList<PartyMember> testMembers = FXCollections.observableArrayList();
    testMembers.add(PartyMemberBuilder.create(PlayerBeanBuilder.create().defaultValues().username("member1").get())
        .get());
    testMembers.add(PartyMemberBuilder.create(PlayerBeanBuilder.create().defaultValues().username("member2").get())
        .get());
    testMembers.add(PartyMemberBuilder.create(PlayerBeanBuilder.create().defaultValues().username("member3").get())
        .get());
    testMembers.add(new PartyMember(player));
    instance.getParty().setMembers(testMembers);
    instance.getParty().setOwner(player);
  }

  private void setOwnerByName(String owner) {
    instance.getParty()
        .getMembers()
        .stream()
        .filter(member -> member.getPlayer().getUsername().equals(owner))
        .findFirst()
        .ifPresent(partyMember1 -> instance.getParty().setOwner(partyMember1.getPlayer()));
  }

  @Test
  public void testOnInviteMessage() {
    PartyInvite message = new PartyInvite(1);

    inviteTestPublisher.next(message);

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

    kickTestPublisher.next(new PartyKick());

    assertThat(instance.getParty().getMembers().size(), is(1));
    assertThat(instance.getParty().getOwner(), is(player));
    assertThat(instance.partyMembersNotReady(), is(false));
  }

  @Test
  public void testOnKickedFromPartyMessageWhenInGame() {
    setPartyMembers();
    setOwnerByName("member2");
    player.setGame(GameBeanBuilder.create().defaultValues().get());

    kickTestPublisher.next(new PartyKick());

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

    partyInfoTestPublisher.next(message);

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
  public void testPartyMembersReady() {
    List<PartyInfo.PartyMember> testMembers = generatePartyMembers(List.of(1, 2));
    PartyInfo message = new PartyInfo(2, testMembers);

    instance.onPartyInfo(message);

    assertThat(instance.partyMembersNotReady(), is(false));

    instance.getParty().getMembers().get(0).getPlayer().setGame(GameBeanBuilder.create().defaultValues().get());

    assertThat(instance.partyMembersNotReady(), is(true));
  }

  @Test
  public void testOnSearchInfoMessage() {
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    SearchInfo message = new SearchInfo("notExistingQueue", MatchmakerState.START);

    searchInfoTestPublisher.next(message);

    verify(gameService, never()).startSearchMatchmaker();

    SearchInfo message2 = new SearchInfo("queue1", MatchmakerState.START);

    searchInfoTestPublisher.next(message2);

    verify(gameService).startSearchMatchmaker();
    assertThat(instance.isInQueue(), is(true));
    assertThat(instance.getQueues().get(0).isSelected(), is(true));
  }

  @Test
  public void testOnMatchFoundMessage() {
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());
    MatchmakerMatchFoundResponse message = new MatchmakerMatchFoundResponse("queue1");

    matchmakerFoundTestPublisher.next(message);

    ArgumentCaptor<TransientNotification> captor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captor.capture());
    assertThat(instance.getQueues().get(0).getMatchingStatus(), is(MatchingStatus.MATCH_FOUND));
    assertThat(instance.getQueues().get(1).getMatchingStatus(), is(nullValue()));
    assertThat(instance.isInQueue(), is(false));
  }

  @Test
  public void testOnMatchCancelledMessage() {
    testOnMatchFoundMessage();

    matchmakerCancelledTestPublisher.next(new MatchmakerMatchCancelledResponse());

    assertThat(instance.getQueues().get(0).getMatchingStatus(), is(MatchingStatus.MATCH_CANCELLED));
    assertThat(instance.getQueues().get(1).getMatchingStatus(), is(nullValue()));
  }

  @Test
  public void testOnGameLaunchMessage() {
    testOnMatchFoundMessage();
    GameLaunchResponse message = GameLaunchMessageBuilder.create()
        .defaultValues()
        .gameType(GameType.MATCHMAKER)
        .initMode(LobbyMode.AUTO_LOBBY)
        .get();

    gameLaunchResponseTestPublisher.next(message);

    assertThat(instance.getQueues().get(0).getMatchingStatus(), is(MatchingStatus.GAME_LAUNCHING));
    assertThat(instance.getQueues().get(1).getMatchingStatus(), is(nullValue()));
  }

  @Test
  public void testOnMatchmakerInfoMessage() {
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("technicalName").eq("queue1"))));
  }

  @NotNull
  private MatchmakerInfo createMatchmakerInfoMessage() {
    MatchmakerInfo.MatchmakerQueue messageQueue1 = new MatchmakerInfo.MatchmakerQueue("queue1", OffsetDateTime.ofInstant(Instant.now()
        .plusSeconds(10), ZoneOffset.UTC), 10, 1, 0, List.of(), List.of());
    MatchmakerInfo.MatchmakerQueue messageQueue2 = new MatchmakerInfo.MatchmakerQueue("queue2", OffsetDateTime.ofInstant(Instant.now()
        .plusSeconds(10), ZoneOffset.UTC), 10, 1, 0, List.of(), List.of());
    ObservableList<MatchmakerInfo.MatchmakerQueue> queues = FXCollections.observableArrayList();
    queues.addAll(messageQueue1, messageQueue2);

    MatchmakerQueue matchmakerQueue1 = new MatchmakerQueue().setId("1")
        .setTechnicalName("queue1")
        .setLeaderboard(new Leaderboard());
    MatchmakerQueue matchmakerQueue2 = new MatchmakerQueue().setId("2")
        .setTechnicalName("queue2")
        .setLeaderboard(new Leaderboard());
    when(fafApiAccessor.getMany(any(ElideNavigatorOnCollection.class))).thenReturn(Flux.just(matchmakerQueue1), Flux.just(matchmakerQueue2));
    return new MatchmakerInfo(queues);
  }

  @Test
  public void testInvitePlayer() {
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    when(playerService.getPlayerByNameIfOnline("invitee")).thenReturn(Optional.of(otherPlayer));

    instance.invitePlayer("invitee");

    verify(fafServerAccessor).inviteToParty(otherPlayer);

    searchInfoTestPublisher.next(new SearchInfo("queue1", MatchmakerState.START));

    instance.invitePlayer("invitee");

    verify(notificationService).addImmediateWarnNotification(any());

    searchInfoTestPublisher.next(new SearchInfo("queue1", MatchmakerState.STOP));

    instance.invitePlayer("invitee");

    verify(fafServerAccessor, times(2)).inviteToParty(otherPlayer);

    instance.setSearching(true);

    instance.invitePlayer("invitee");

    verify(notificationService, times(2)).addImmediateWarnNotification(any());
  }

  @Test
  public void testAcceptInvite() {
    instance.acceptPartyInvite(player);

    verify(fafServerAccessor).acceptPartyInvite(player);
    verify(navigationHandler).navigateTo(any(OpenTeamMatchmakingEvent.class));
  }

  @Test
  public void testAcceptInvitePlayerInQueue() {
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());
    searchInfoTestPublisher.next(new SearchInfo("queue1", MatchmakerState.START));

    instance.acceptPartyInvite(player);

    verify(notificationService).addImmediateWarnNotification("teammatchmaking.notification.joinAlreadyInQueue.message");
  }

  @Test
  public void testAcceptInvitePlayerSearching() {
    instance.setSearching(true);

    instance.acceptPartyInvite(player);

    verify(notificationService).addImmediateWarnNotification("teammatchmaking.notification.joinAlreadyInQueue.message");
  }

  @Test
  public void testAcceptInviteGameRunning() {
    when(gameService.isGameRunning()).thenReturn(true);

    instance.acceptPartyInvite(player);

    verify(notificationService).addImmediateWarnNotification("teammatchmaking.notification.gameRunning.message");
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
  public void testJoinLeaveQueues() {
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    when(mapService.downloadAllMatchmakerMaps(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(new FeaturedModBean()));
    when(gameService.updateGameIfNecessary(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    Boolean success = instance.joinQueues().join();

    verify(fafServerAccessor, times(2)).gameMatchmaking(any(), eq(MatchmakerState.START));
    assertThat(success, is(true));

    SearchInfo message1 = new SearchInfo("queue1", MatchmakerState.START);
    SearchInfo message2 = new SearchInfo("queue2", MatchmakerState.START);
    searchInfoTestPublisher.next(message1, message2);

    instance.leaveQueues();

    verify(fafServerAccessor, times(2)).gameMatchmaking(any(), eq(MatchmakerState.STOP));
  }

  @Test
  public void testJoinLeaveQueuesUnselectedQueues() {
    matchmakerPrefs.getUnselectedQueueIds().add(1);
    matchmakerPrefs.getUnselectedQueueIds().add(2);

    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    when(mapService.downloadAllMatchmakerMaps(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(new FeaturedModBean()));
    when(gameService.updateGameIfNecessary(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    Boolean success = instance.joinQueues().join();

    verify(fafServerAccessor, never()).gameMatchmaking(any(), eq(MatchmakerState.START));
    assertThat(success, is(false));

    instance.leaveQueues();

    verify(fafServerAccessor, never()).gameMatchmaking(any(), eq(MatchmakerState.STOP));
  }

  @Test
  public void testJoinLeaveQueuesPartyTooBig() {
    setPartyMembers();

    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    when(mapService.downloadAllMatchmakerMaps(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(new FeaturedModBean()));
    when(gameService.updateGameIfNecessary(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    Boolean success = instance.joinQueues().join();

    verify(fafServerAccessor, never()).gameMatchmaking(any(), eq(MatchmakerState.START));
    assertThat(success, is(false));

    instance.leaveQueues();

    verify(fafServerAccessor, never()).gameMatchmaking(any(), eq(MatchmakerState.STOP));
  }

  @Test
  public void testJoinLeaveQueuesPartyNotOwner() {
    instance.getParty().setOwner(null);

    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    when(mapService.downloadAllMatchmakerMaps(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(new FeaturedModBean()));
    when(gameService.updateGameIfNecessary(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    Boolean success = instance.joinQueues().join();

    verify(fafServerAccessor, never()).gameMatchmaking(any(), eq(MatchmakerState.START));
    assertThat(success, is(false));

    instance.leaveQueues();

    verify(notificationService, times(2)).addImmediateWarnNotification(anyString());
    verify(fafServerAccessor, never()).gameMatchmaking(any(), eq(MatchmakerState.STOP));
  }

  @Test
  public void testJoinQueuesPartyJoinQueueFails() {
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());

    when(mapService.downloadAllMatchmakerMaps(any())).thenReturn(CompletableFuture.failedFuture(new Exception()));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(new FeaturedModBean()));
    when(gameService.updateGameIfNecessary(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    Boolean success = instance.joinQueues().join();

    verify(notificationService, times(2)).addImmediateErrorNotification(any(), anyString(), anyString());
    verify(fafServerAccessor, never()).gameMatchmaking(any(), eq(MatchmakerState.START));
    assertThat(success, is(false));
  }

  @Test
  public void testJoinQueuesFailed() {
    MatchmakerQueueBean queue = new MatchmakerQueueBean();
    when(mapService.downloadAllMatchmakerMaps(queue)).thenReturn(CompletableFuture.completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(new FeaturedModBean()));
    when(gameService.updateGameIfNecessary(any(), any())).thenReturn(CompletableFuture.failedFuture(new Exception()));

    Boolean success = instance.joinQueues().join();

    verify(notificationService).addImmediateErrorNotification(any(), anyString());
    verify(fafServerAccessor, never()).gameMatchmaking(queue, MatchmakerState.START);
    assertThat(success, is(false));
  }

  @Test
  public void testJoinQueueWhileGameRunning() {
    when(gameService.isGameRunning()).thenReturn(true);

    Boolean success = instance.joinQueues().join();

    verify(notificationService).addImmediateWarnNotification(anyString());
    assertThat(success, is(false));
  }

  @Test
  public void testSelectDeselectQueuesWhileSearching() {
    instance.getParty().setOwner(player);
    matchmakerInfoTestPublisher.next(createMatchmakerInfoMessage());
    instance.setSearching(true);

    when(mapService.downloadAllMatchmakerMaps(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(modService.getFeaturedMod(anyString())).thenReturn(Mono.just(new FeaturedModBean()));
    when(gameService.updateGameIfNecessary(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    SearchInfo message1 = new SearchInfo("queue1", MatchmakerState.START);
    SearchInfo message2 = new SearchInfo("queue2", MatchmakerState.START);
    searchInfoTestPublisher.next(message1, message2);

    instance.getQueues().forEach(queue -> queue.setSelected(false));

    assertThat(matchmakerPrefs.getUnselectedQueueIds(), hasItems(1, 2));
    verify(fafServerAccessor, times(2)).gameMatchmaking(any(), eq(MatchmakerState.STOP));

    SearchInfo message3 = new SearchInfo("queue1", MatchmakerState.STOP);
    SearchInfo message4 = new SearchInfo("queue2", MatchmakerState.STOP);
    searchInfoTestPublisher.next(message3, message4);

    instance.setSearching(true);
    instance.getQueues().forEach(queue -> queue.setSelected(true));

    Assertions.assertTrue(matchmakerPrefs.getUnselectedQueueIds().isEmpty());
    verify(fafServerAccessor, times(2)).gameMatchmaking(any(), eq(MatchmakerState.START));
  }

  @Test
  public void testSendFactionsOnConnected() {
    connectionState.set(ConnectionState.CONNECTED);

    verify(fafServerAccessor).setPartyFactions(anyList());
  }
}
