package com.faforever.client.teammatchmaking;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueBean.MatchingStatus;
import com.faforever.client.domain.PartyBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fx.JavaFxService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.game.GameService;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MatchmakerMapper;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.Action.ActionCallback;
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
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
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
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamMatchmakingService implements InitializingBean {

  private final MapService mapService;
  private final ModService modService;
  private final PlayerService playerService;
  private final NotificationService notificationService;
  private final PreferencesService preferencesService;
  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final EventBus eventBus;
  private final I18n i18n;
  private final TaskScheduler taskScheduler;
  private final GameService gameService;
  private final JavaFxService javaFxService;
  private final MatchmakerMapper matchmakerMapper;

  @Getter
  private final PartyBean party = new PartyBean();
  private final ObservableMap<String, MatchmakerQueueBean> nameToQueue = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  @Getter
  private final ObservableList<MatchmakerQueueBean> queues = JavaFxUtil.attachListToMap(FXCollections.synchronizedObservableList(FXCollections.observableArrayList(queue -> new Observable[]{queue.joinedProperty()})), nameToQueue);
  private final ReadOnlyBooleanWrapper partyMembersNotReady = new ReadOnlyBooleanWrapper();
  private final SetProperty<PlayerBean> playersInGame = new SimpleSetProperty<>(FXCollections.observableSet());
  private final List<ScheduledFuture<?>> leaveQueueTimeouts = new LinkedList<>();

  private final AtomicBoolean matchFoundAndWaitingForGameLaunch = new AtomicBoolean();
  private final BooleanProperty currentlyInQueue = new SimpleBooleanProperty();

  private CompletableFuture<Void> matchmakingGameFuture;

  @Override
  public void afterPropertiesSet() throws Exception {
    fafServerAccessor.addEventListener(SearchInfo.class, this::onSearchInfoMessage);
    fafServerAccessor.addEventListener(GameLaunchResponse.class, this::onGameLaunchMessage);

    queues.addListener((SimpleInvalidationListener) () -> currentlyInQueue.set(queues.stream()
        .anyMatch(MatchmakerQueueBean::isJoined)));

    fafServerAccessor.getEvents(PartyInfo.class)
        .publishOn(javaFxService.getFxApplicationScheduler())
        .doOnNext(this::onPartyInfo)
        .doOnError(throwable -> log.error("Error processing party info", throwable))
        .retry()
        .subscribe();

    fafServerAccessor.getEvents(PartyKick.class)
        .publishOn(javaFxService.getFxApplicationScheduler())
        .doOnNext(message -> initializeParty())
        .doOnError(throwable -> log.error("Error processing party kick", throwable))
        .retry()
        .subscribe();

    fafServerAccessor.getEvents(PartyInvite.class)
        .map(message -> playerService.getPlayerByIdIfOnline(message.getSender()))
        .flatMap(Mono::justOrEmpty)
        .doOnNext(this::sendInviteNotifications)
        .doOnError(throwable -> log.error("Error processing invite", throwable))
        .retry()
        .subscribe();

    fafServerAccessor.getEvents(MatchmakerMatchFoundResponse.class)
        .doOnNext(ignored -> matchFoundAndWaitingForGameLaunch.set(true))
        .doOnNext(ignored -> notifyMatchFound())
        .publishOn(javaFxService.getFxApplicationScheduler())
        .doOnNext(this::setFoundLabelForQueue)
        .doOnNext(ignored -> queues.forEach(matchmakingQueue -> matchmakingQueue.setJoined(false)))
        .publishOn(javaFxService.getSingleScheduler())
        .doOnError(throwable -> log.error("Error processing found response", throwable))
        .retry()
        .subscribe();

    fafServerAccessor.getEvents(MatchmakerMatchCancelledResponse.class)
        .publishOn(javaFxService.getFxApplicationScheduler())
        .doOnNext(ignored -> changeLabelForQueues(MatchingStatus.MATCH_CANCELLED))
        .publishOn(javaFxService.getSingleScheduler())
        .doOnError(throwable -> log.error("Error handling cancelled response", throwable))
        .retry()
        .subscribe(ignored -> {
          matchFoundAndWaitingForGameLaunch.set(false);
          stopMatchMakerLaunch();
        });

    fafServerAccessor.getEvents(MatchmakerInfo.class)
        .flatMapIterable(MatchmakerInfo::getQueues)
        .concatMap(matchmakerQueue -> Mono.zip(Mono.just(matchmakerQueue), Mono.justOrEmpty(nameToQueue.get(matchmakerQueue.getName()))
            .switchIfEmpty(getQueueFromApi(matchmakerQueue.getName()))))
        .publishOn(javaFxService.getFxApplicationScheduler())
        .map(TupleUtils.function(matchmakerMapper::update))
        .doOnError(throwable -> log.error("Error updating queue", throwable))
        .retry()
        .subscribe();

    partyMembersNotReady.bind(playersInGame.emptyProperty().map(empty -> !empty));

    eventBus.register(this);
  }

  private void sendInviteNotifications(PlayerBean player) {
    ActionCallback callback = event -> this.acceptPartyInvite(player);

    notificationService.addNotification(new TransientNotification(i18n.get("teammatchmaking.notification.invite.title"), i18n.get("teammatchmaking.notification.invite.message", player.getUsername()), IdenticonUtil.createIdenticon(player.getId()), callback));
    notificationService.addNotification(new PersistentNotification(i18n.get("teammatchmaking.notification.invite.message", player.getUsername()), Severity.INFO, Collections.singletonList(new Action(i18n.get("teammatchmaking.notification.invite.join"), callback))));
  }

  private void changeLabelForQueues(MatchingStatus matchCancelled) {
    queues.stream()
        .filter(matchmakingQueue -> matchmakingQueue.getMatchingStatus() != null)
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(matchCancelled, Duration.ofSeconds(60), taskScheduler));
  }

  private void notifyMatchFound() {
    notificationService.addNotification(new TransientNotification(i18n.get("teammatchmaking.notification.matchFound.title"), i18n.get("teammatchmaking.notification.matchFound.message")));
  }

  private void setFoundLabelForQueue(MatchmakerMatchFoundResponse message) {
    queues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), message.getQueueName()))
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(MatchingStatus.MATCH_FOUND, Duration.ofSeconds(60), taskScheduler));
  }

  private void updateMatchmakerGameCount(MatchmakerQueueBean matchmakerQueue) {
    String leaderboard = matchmakerQueue.getLeaderboard().getTechnicalName();
    int activeGames = (int) gameService.getGames()
        .stream()
        .filter(game -> GameStatus.CLOSED != game.getStatus())
        .filter(game -> GameType.MATCHMAKER == game.getGameType())
        .filter(game -> leaderboard.equals(game.getLeaderboard()))
        .count();
    matchmakerQueue.setActiveGames(activeGames);
  }

  private Mono<MatchmakerQueueBean> getQueueFromApi(String queueTechnicalName) {
    ElideNavigatorOnCollection<MatchmakerQueue> navigator = ElideNavigator.of(MatchmakerQueue.class)
        .collection()
        .setFilter(qBuilder().string("technicalName").eq(queueTechnicalName));
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> matchmakerMapper.map(dto, new CycleAvoidingMappingContext()))
        .doOnNext(queue -> JavaFxUtil.addAndTriggerListener(gameService.getGames(), (SimpleInvalidationListener) () -> updateMatchmakerGameCount(queue)))
        .doOnNext(queue -> nameToQueue.put(queue.getTechnicalName(), queue));
  }

  @VisibleForTesting
  protected void onSearchInfoMessage(SearchInfo message) {
    queues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), message.getQueueName()))
        .forEach(matchmakingQueue -> {
          JavaFxUtil.runLater(() -> matchmakingQueue.setJoined(message.getState().equals(MatchmakerState.START)));
          leaveQueueTimeouts.forEach(f -> f.cancel(false));

          if (message.getState().equals(MatchmakerState.START)) {
            party.getMembers()
                .stream()
                .filter(partyMember -> Objects.equals(partyMember.getPlayer(), playerService.getCurrentPlayer()))
                .findFirst()
                .ifPresent(member -> sendFactionSelection(member.getFactions()));

            matchmakingGameFuture = gameService.startSearchMatchmaker();
          }
        });

    if (queues.stream().noneMatch(MatchmakerQueueBean::isJoined) && !matchFoundAndWaitingForGameLaunch.get()) {
      stopMatchMakerLaunch();
    }
  }

  private void stopMatchMakerLaunch() {
    if (matchmakingGameFuture != null) {
      matchmakingGameFuture.cancel(false);
    }
  }

  @VisibleForTesting
  protected void onGameLaunchMessage(GameLaunchResponse message) {
    if (message.getGameType() != GameType.MATCHMAKER) {
      return;
    }

    changeLabelForQueues(MatchingStatus.GAME_LAUNCHING);

    matchFoundAndWaitingForGameLaunch.set(false);
  }

  public CompletableFuture<Boolean> joinQueue(MatchmakerQueueBean queue) {
    if (gamePathInvalid()) {
      return CompletableFuture.completedFuture(false);
    }

    if (gameService.isGameRunning()) {
      log.debug("Game is running, ignoring tmm queue join request");
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.gameAlreadyRunning.message");
      return CompletableFuture.completedFuture(false);
    }

    return mapService.downloadAllMatchmakerMaps(queue)
        .thenCompose(aVoid -> modService.getFeaturedMod(FAF.getTechnicalName()))
        .thenCompose(featuredModBean -> gameService.updateGameIfNecessary(featuredModBean, Set.of()))
        .thenRun(() -> fafServerAccessor.gameMatchmaking(queue, MatchmakerState.START))
        .thenApply(aVoid -> true)
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          log.error("Unable to join queue", throwable);
          if (throwable instanceof NotifiableException) {
            notificationService.addErrorNotification((NotifiableException) throwable);
          } else {
            notificationService.addImmediateErrorNotification(throwable, "game.start.couldNotStart");
          }
          return false;
        });
  }

  public void leaveQueue(MatchmakerQueueBean queue) {
    fafServerAccessor.gameMatchmaking(queue, MatchmakerState.STOP);
    leaveQueueTimeouts.add(taskScheduler.schedule(() -> JavaFxUtil.runLater(() -> queue.setJoined(false)), Instant.now()
        .plus(Duration.ofSeconds(5))));
  }

  public void onPartyInfo(PartyInfo message) {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    if (message.getMembers().stream().noneMatch(partyMember -> partyMember.getPlayerId() == currentPlayer.getId())) {
      initializeParty();
      return;
    }
    setPartyFromInfoMessage(message);
  }

  @VisibleForTesting
  void acceptPartyInvite(PlayerBean player) {
    if (isCurrentlyInQueue()) {
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.joinAlreadyInQueue.message");
      return;
    }

    if (gameService.isGameRunning()) {
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.gameRunning.message");
      return;
    }

    if (gamePathInvalid()) {
      return;
    }

    fafServerAccessor.acceptPartyInvite(player);
    eventBus.post(new OpenTeamMatchmakingEvent());
  }

  public void invitePlayer(String player) {
    if (isCurrentlyInQueue()) {
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.inviteAlreadyInQueue.message");
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
    party.getMembers()
        .stream()
        .filter(partyMember -> partyMember.getPlayer().equals(currentPlayer))
        .findFirst()
        .ifPresent(partyMember -> {
          newPartyMember.setFactions(partyMember.getFactions());
          sendFactionSelection(partyMember.getFactions());
        });

    party.setOwner(currentPlayer);
    party.getMembers().forEach(partyMember -> partyMember.setGameStatusChangeListener(null));
    party.setMembers(List.of(newPartyMember));
    playersInGame.clear();
    if (currentPlayer != null && currentPlayer.getStatus() != PlayerStatus.IDLE) {
      playersInGame.add(currentPlayer);
    }

    eventBus.post(new PartyOwnerChangedEvent(currentPlayer));
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
    List<PartyMember> members = message.getMembers()
        .stream()
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
    nameToQueue.clear();
  }

  @Subscribe
  public void onLoggedIn(LoginSuccessEvent event) {
    initializeParty();
  }
}
