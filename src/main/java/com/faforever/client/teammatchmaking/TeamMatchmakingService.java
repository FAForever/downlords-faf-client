package com.faforever.client.teammatchmaking;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueBean.MatchingStatus;
import com.faforever.client.domain.PartyBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GameService;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MatchmakerMapper;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.teammatchmaking.event.PartyOwnerChangedEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@Service
@Slf4j
public class TeamMatchmakingService implements InitializingBean {

  private final PlayerService playerService;
  private final NotificationService notificationService;
  private final PreferencesService preferencesService;
  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final EventBus eventBus;
  private final I18n i18n;
  private final TaskScheduler taskScheduler;
  private final GameService gameService;
  private final MatchmakerMapper matchmakerMapper;

  @Getter
  private final PartyBean party;
  @Getter
  private final ObservableList<MatchmakerQueueBean> matchmakerQueues = FXCollections.observableArrayList();
  private final ObservableMap<Integer, MatchmakerQueueBean> queueIdToQueue = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  private final ReadOnlyBooleanWrapper partyMembersNotReady;
  private final ObservableSet<PlayerBean> playersInGame;
  private final List<ScheduledFuture<?>> leaveQueueTimeouts = new LinkedList<>();

  private volatile boolean matchFoundAndWaitingForGameLaunch = false;
  private final BooleanProperty currentlyInQueue = new SimpleBooleanProperty();
  private final InvalidationListener queueJoinInvalidationListener;

  private CompletableFuture<Void> matchmakingGameFuture;

  public TeamMatchmakingService(PlayerService playerService, NotificationService notificationService, PreferencesService preferencesService, FafApiAccessor fafApiAccessor, FafServerAccessor fafServerAccessor, EventBus eventBus, I18n i18n, TaskScheduler taskScheduler, GameService gameService, MatchmakerMapper matchmakerMapper) {
    this.playerService = playerService;
    this.notificationService = notificationService;
    this.preferencesService = preferencesService;
    this.fafApiAccessor = fafApiAccessor;
    this.fafServerAccessor = fafServerAccessor;
    this.eventBus = eventBus;
    this.i18n = i18n;
    this.taskScheduler = taskScheduler;
    this.gameService = gameService;
    this.matchmakerMapper = matchmakerMapper;

    fafServerAccessor.addEventListener(PartyInvite.class, this::onPartyInvite);
    fafServerAccessor.addEventListener(PartyKick.class, this::onPartyKicked);
    fafServerAccessor.addEventListener(PartyInfo.class, this::onPartyInfo);
    fafServerAccessor.addEventListener(SearchInfo.class, this::onSearchInfoMessage);
    fafServerAccessor.addEventListener(MatchmakerMatchFoundResponse.class, this::onMatchFoundMessage);
    fafServerAccessor.addEventListener(MatchmakerMatchCancelledResponse.class, this::onMatchCancelledMessage);
    fafServerAccessor.addEventListener(GameLaunchResponse.class, this::onGameLaunchMessage);
    fafServerAccessor.addEventListener(MatchmakerInfo.class, this::onMatchmakerInfo);

    party = new PartyBean();

    playersInGame = FXCollections.observableSet();
    partyMembersNotReady = new ReadOnlyBooleanWrapper();

    partyMembersNotReady.bind(Bindings.createBooleanBinding(() -> !playersInGame.isEmpty(), playersInGame));

    queueJoinInvalidationListener = observable -> currentlyInQueue.set(matchmakerQueues.stream().anyMatch(MatchmakerQueueBean::isJoined));

    JavaFxUtil.attachListToMap(matchmakerQueues, queueIdToQueue);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  @VisibleForTesting
  protected void onMatchmakerInfo(MatchmakerInfo message) {
    message.getQueues().forEach(this::updateOrCreateQueue);
  }

  private synchronized void updateOrCreateQueue(MatchmakerInfo.MatchmakerQueue messageQueue) {
    matchmakerQueues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), messageQueue.getName()))
        .findFirst()
        .ifPresentOrElse(matchmakerQueue -> matchmakerMapper.update(messageQueue, matchmakerQueue),
            () -> getQueueFromApi(messageQueue.getName()).thenAccept(matchmakerQueueFromApi ->
                matchmakerQueueFromApi.ifPresent(matchmakerQueue -> {
                  queueIdToQueue.put(matchmakerQueue.getId(), matchmakerQueue);
                  matchmakerQueue.joinedProperty().addListener(queueJoinInvalidationListener);
                  matchmakerMapper.update(messageQueue, matchmakerQueue);
                })));
  }

  private CompletableFuture<Optional<MatchmakerQueueBean>> getQueueFromApi(String queueTechnicalName) {
    ElideNavigatorOnCollection<MatchmakerQueue> navigator = ElideNavigator.of(MatchmakerQueue.class).collection()
        .setFilter(qBuilder().string("technicalName").eq(queueTechnicalName));
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> matchmakerMapper.map(dto, new CycleAvoidingMappingContext()))
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  @VisibleForTesting
  protected void onSearchInfoMessage(SearchInfo message) {
    matchmakerQueues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), message.getQueueName()))
        .forEach(matchmakingQueue -> {
          matchmakingQueue.setJoined(message.getState().equals(MatchmakerState.START));
          leaveQueueTimeouts.forEach(f -> f.cancel(false));

              if (message.getState().equals(MatchmakerState.START)) {
                party.getMembers().stream()
                    .filter(partyMember -> Objects.equals(partyMember.getPlayer(), playerService.getCurrentPlayer()))
                    .findFirst()
                    .ifPresent(member -> sendFactionSelection(member.getFactions()));

                matchmakingGameFuture = gameService.startSearchMatchmaker();
              }
            }
        );

    if (matchmakerQueues.stream().noneMatch(MatchmakerQueueBean::isJoined) && !matchFoundAndWaitingForGameLaunch) {
      stopMatchMakerLaunch();
    }
  }

  private void stopMatchMakerLaunch() {
    if (matchmakingGameFuture != null) {
      matchmakingGameFuture.cancel(false);
    }
  }

  @VisibleForTesting
  protected void onMatchFoundMessage(MatchmakerMatchFoundResponse message) {
    matchFoundAndWaitingForGameLaunch = true; // messages from server: match found -> STOP all queues that you are in that haven't found a match -> game launch

    notificationService.addNotification(new TransientNotification(
        i18n.get("teammatchmaking.notification.matchFound.title"),
        i18n.get("teammatchmaking.notification.matchFound.message")
    ));

    matchmakerQueues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), message.getQueueName()))
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(MatchingStatus.MATCH_FOUND, Duration.ofSeconds(60), taskScheduler));

    matchmakerQueues.forEach(matchmakingQueue -> matchmakingQueue.setJoined(false));
  }

  @VisibleForTesting
  protected void onMatchCancelledMessage(MatchmakerMatchCancelledResponse message) {
    matchmakerQueues.stream()
        .filter(matchmakingQueue -> matchmakingQueue.getMatchingStatus() != null)
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(MatchingStatus.MATCH_CANCELLED, Duration.ofSeconds(60), taskScheduler));

    matchFoundAndWaitingForGameLaunch = false;
    stopMatchMakerLaunch();
  }

  @VisibleForTesting
  protected void onGameLaunchMessage(GameLaunchResponse message) {
    if (message.getLobbyMode() != LobbyMode.AUTO_LOBBY) {
      return;
    }

    matchmakerQueues.stream()
        .filter(matchmakingQueue -> matchmakingQueue.getMatchingStatus() != null)
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(MatchingStatus.GAME_LAUNCHING, Duration.ofSeconds(60), taskScheduler));

    matchFoundAndWaitingForGameLaunch = false;
  }

  public boolean joinQueue(MatchmakerQueueBean queue) {
    if (gamePathInvalid()) {
      return false;
    }

    if (gameService.isGameRunning()) {
      log.debug("Game is running, ignoring tmm queue join request");
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("teammatchmaking.notification.gameAlreadyRunning.title"),
          i18n.get("teammatchmaking.notification.gameAlreadyRunning.message"),
          Severity.WARN,
          Collections.singletonList(new Action(i18n.get("dismiss")))
      ));
      return false;
    }

    fafServerAccessor.gameMatchmaking(queue, MatchmakerState.START);
    return true;
  }

  public void leaveQueue(MatchmakerQueueBean queue) {
    fafServerAccessor.gameMatchmaking(queue, MatchmakerState.STOP);
    leaveQueueTimeouts.add(taskScheduler.schedule(
        () -> JavaFxUtil.runLater(() -> queue.setJoined(false)), Instant.now().plus(Duration.ofSeconds(5))));
  }

  public void onPartyInfo(PartyInfo message) {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    if (message.getMembers().stream().noneMatch(partyMember -> partyMember.getPlayerId() == currentPlayer.getId())) {
      initializeParty();
      return;
    }
    setPartyFromInfoMessage(message);
  }

  public void onPartyInvite(PartyInvite message) {
    playerService.getPlayerByIdIfOnline(message.getSender())
        .ifPresentOrElse(player -> {
          ActionCallback callback = event -> this.acceptPartyInvite(player);

          notificationService.addNotification(new TransientNotification(
              i18n.get("teammatchmaking.notification.invite.title"),
              i18n.get("teammatchmaking.notification.invite.message", player.getUsername()),
              IdenticonUtil.createIdenticon(player.getId()),
              callback
          ));

          notificationService.addNotification(new PersistentNotification(
              i18n.get("teammatchmaking.notification.invite.message", player.getUsername()),
              Severity.INFO,
              Collections.singletonList(new Action(
                  i18n.get("teammatchmaking.notification.invite.join"),
                  callback
              ))
          ));
        }, () -> log.error("User {} of party invite not found", message.getSender()));
  }

  @VisibleForTesting
  protected void acceptPartyInvite(PlayerBean player) {
    if (isCurrentlyInQueue()) {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("teammatchmaking.notification.joinAlreadyInQueue.title"),
          i18n.get("teammatchmaking.notification.joinAlreadyInQueue.message"),
          Severity.WARN,
          Collections.singletonList(new Action(i18n.get("dismiss")))
      ));
      return;
    }

    if (gamePathInvalid()) {
      return;
    }

    fafServerAccessor.acceptPartyInvite(player);
    eventBus.post(new OpenTeamMatchmakingEvent());
  }

  public void onPartyKicked(PartyKick message) {
    initializeParty();
  }

  public void invitePlayer(String player) {
    if (isCurrentlyInQueue()) {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("teammatchmaking.notification.inviteAlreadyInQueue.title"),
          i18n.get("teammatchmaking.notification.inviteAlreadyInQueue.message"),
          Severity.WARN,
          Collections.singletonList(new Action(i18n.get("dismiss")))
      ));
      return;
    }

    if (gamePathInvalid()) {
      return;
    }

    playerService.getPlayerByNameIfOnline(player).ifPresent(fafServerAccessor::inviteToParty);
  }

  public void kickPlayerFromParty(PlayerBean player) {
    fafServerAccessor.kickPlayerFromParty(player);
    playersInGame.remove(player);
  }

  public void leaveParty() {
    fafServerAccessor.leaveParty();
    initializeParty();
  }

  private void initializeParty() {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    PartyMember newPartyMember = new PartyMember(currentPlayer);
    party.getMembers().stream()
        .filter(partyMember -> partyMember.getPlayer().equals(currentPlayer))
        .findFirst()
        .ifPresent(partyMember -> {
          newPartyMember.setFactions(partyMember.getFactions());
          sendFactionSelection(partyMember.getFactions());
        });

    party.setOwner(currentPlayer);
    eventBus.post(new PartyOwnerChangedEvent(currentPlayer));
    party.getMembers().forEach(partyMember -> partyMember.setGameStatusChangeListener(null));
    party.setMembers(List.of(newPartyMember));
    playersInGame.clear();
    if (currentPlayer != null && !Objects.equals(currentPlayer.getStatus(), PlayerStatus.IDLE)) {
      playersInGame.add(currentPlayer);
    }
  }

  public void readyParty() {
    fafServerAccessor.readyParty();
  }

  public void unreadyParty() {
    fafServerAccessor.unreadyParty();
  }

  public void sendFactionSelection(List<Faction> factions) {
    fafServerAccessor.setPartyFactions(factions);
  }

  private boolean gamePathInvalid() {
    if (!preferencesService.isGamePathValid()) {
      eventBus.post(new MissingGamePathEvent(true));
      return true;
    }
    return false;
  }

  public boolean isCurrentlyInQueue() {
    return currentlyInQueue.get();
  }

  public BooleanProperty currentlyInQueueProperty() {
    return currentlyInQueue;
  }

  private synchronized void setPartyFromInfoMessage(PartyInfo message) {
    setOwnerFromInfoMessage(message);
    setMembersFromInfoMessage(message);
  }

  private synchronized void setOwnerFromInfoMessage(PartyInfo message) {
    playerService.getPlayerByIdIfOnline(message.getOwner()).ifPresent(player -> {
      party.setOwner(player);
      eventBus.post(new PartyOwnerChangedEvent(player));
    });
  }

  private synchronized void setMembersFromInfoMessage(PartyInfo message) {
    playersInGame.clear();
    List<PartyMember> members = message.getMembers().stream()
        .map(this::createPartyMemberFromOnlinePLayers)
        .filter(Objects::nonNull)
        .peek(partyMember -> partyMember.setGameStatusChangeListener(observable -> checkMemberGameStatus(partyMember)))
        .collect(Collectors.toList());
    party.getMembers().forEach(partyMember -> partyMember.setGameStatusChangeListener(null));
    party.setMembers(members);
  }

  private void checkMemberGameStatus(PartyMember partyMember) {
    PlayerBean memberPlayer = partyMember.getPlayer();
    if (memberPlayer.getStatus() != PlayerStatus.IDLE) {
      playersInGame.add(memberPlayer);
    } else {
      playersInGame.remove(memberPlayer);
    }
  }

  private PartyMember createPartyMemberFromOnlinePLayers(PartyInfo.PartyMember member) {
    return playerService.getPlayerByIdIfOnline(member.getPlayerId())
        .map(player -> new PartyMember(player, member.getFactions()))
        .orElseGet(() -> {
          log.warn("Could not find party member {}", member.getPlayerId());
          return null;
        });
  }

  public boolean partyMembersNotReady() {
    return partyMembersNotReady.get();
  }

  public ReadOnlyBooleanProperty partyMembersNotReadyProperty() {
    return partyMembersNotReady.getReadOnlyProperty();
  }

  public void requestMatchmakerInfo() {
    fafServerAccessor.requestMatchmakerInfo();
  }

  @Subscribe
  public void onLogOut(LoggedOutEvent event) {
    queueIdToQueue.clear();
  }

  @Subscribe
  public void onLoggedIn(LoginSuccessEvent event) {
    initializeParty();
  }
}
