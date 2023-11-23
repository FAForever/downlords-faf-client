package com.faforever.client.teammatchmaking;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.chat.ChatService;
import com.faforever.client.domain.MatchingStatus;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PartyBean;
import com.faforever.client.domain.PartyBean.PartyMember;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.game.GameService;
import com.faforever.client.game.MissingGamePathNotifier;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MatchmakerMapper;
import com.faforever.client.mod.ModService;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.user.LoginService;
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
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.FilteredList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.faforever.client.chat.ChatService.PARTY_CHANNEL_SUFFIX;
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
  private final ChatService chatService;
  private final PreferencesService preferencesService;
  private final LoginService loginService;
  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final NavigationHandler navigationHandler;
  private final I18n i18n;
  private final TaskScheduler taskScheduler;
  private final GameService gameService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final MatchmakerMapper matchmakerMapper;
  private final MatchmakerPrefs matchmakerPrefs;
  private final MissingGamePathNotifier missingGamePathNotifier;

  @Getter
  private final PartyBean party = new PartyBean();
  private final ObservableMap<String, MatchmakerQueueBean> nameToQueue = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  @Getter
  private final ObservableList<MatchmakerQueueBean> queues = JavaFxUtil.attachListToMap(
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList(
          queue -> new Observable[]{queue.selectedProperty(), queue.matchingStatusProperty()})), nameToQueue);
  private final FilteredList<MatchmakerQueueBean> selectedQueues = new FilteredList<>(queues,
                                                                                      MatchmakerQueueBean::isSelected);
  private final FilteredList<MatchmakerQueueBean> validQueues = new FilteredList<>(selectedQueues);
  private final BooleanBinding anyQueueSelected = Bindings.isNotEmpty(selectedQueues);
  private final ReadOnlyBooleanWrapper partyMembersNotReady = new ReadOnlyBooleanWrapper();
  private final SetProperty<PlayerBean> playersInGame = new SimpleSetProperty<>(FXCollections.observableSet());

  private final AtomicBoolean matchFoundAndWaitingForGameLaunch = new AtomicBoolean();
  private final ReadOnlyBooleanWrapper inQueue = new ReadOnlyBooleanWrapper();
  private final BooleanProperty searching = new SimpleBooleanProperty();

  @Override
  public void afterPropertiesSet() throws Exception {
    loginService.loggedInProperty().addListener((SimpleChangeListener<Boolean>) loggedIn -> {
      if (loggedIn) {
        initializeParty();
      } else {
        nameToQueue.clear();
      }
    });

    validQueues.predicateProperty()
               .bind(Bindings.createObjectBinding(() -> queue -> party.getMembers().size() <= queue.getTeamSize(),
                                                  party.getMembers()));

    inQueue.bind(Bindings.createBooleanBinding(
        () -> queues.stream().map(MatchmakerQueueBean::getMatchingStatus).anyMatch(MatchingStatus.SEARCHING::equals),
        queues));

    inQueue.addListener(((SimpleChangeListener<Boolean>) this::onInQueueChange));

    selectedQueues.addListener(this::onSelectedQueueChange);
    validQueues.addListener(this::onValidQueueChange);

    fafServerAccessor.getEvents(GameLaunchResponse.class)
                     .map(GameLaunchResponse::getGameType)
                     .filter(GameType.MATCHMAKER::equals)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .doOnNext(ignored -> changeLabelForQueues(MatchingStatus.GAME_LAUNCHING))
                     .publishOn(Schedulers.single())
                     .doOnNext(ignored -> matchFoundAndWaitingForGameLaunch.set(false))
                     .doOnError(throwable -> log.error("Error processing game launch response", throwable))
                     .retry()
                     .subscribe();

    fafServerAccessor.getEvents(SearchInfo.class)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .doOnNext(this::onSearchInfo)
                     .publishOn(Schedulers.single())
                     .doOnError(throwable -> log.error("Error processing search info", throwable))
                     .retry()
                     .subscribe();

    fafServerAccessor.getEvents(PartyInfo.class)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .doOnNext(this::onPartyInfo)
                     .doOnError(throwable -> log.error("Error processing party info", throwable))
                     .retry()
                     .subscribe();

    fafServerAccessor.getEvents(PartyKick.class)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
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
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .doOnNext(ignored -> queues.forEach(matchmakingQueue -> matchmakingQueue.setMatchingStatus(null)))
                     .doOnNext(ignored -> searching.set(false))
                     .doOnNext(this::setFoundStatusForQueue)
                     .publishOn(Schedulers.single())
                     .doOnError(throwable -> log.error("Error processing found response", throwable))
                     .retry()
                     .subscribe();

    fafServerAccessor.getEvents(MatchmakerMatchCancelledResponse.class)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .doOnNext(ignored -> changeLabelForQueues(MatchingStatus.MATCH_CANCELLED))
                     .publishOn(Schedulers.single())
                     .doOnError(throwable -> log.error("Error handling cancelled response", throwable))
                     .doOnNext(ignored -> matchFoundAndWaitingForGameLaunch.set(false))
                     .doOnNext(ignored -> gameService.stopSearchMatchmaker())
                     .retry()
                     .subscribe();

    fafServerAccessor.getEvents(MatchmakerInfo.class)
                     .flatMapIterable(MatchmakerInfo::getQueues)
                     .concatMap(matchmakerQueue -> Mono.zip(Mono.just(matchmakerQueue),
                                                            Mono.justOrEmpty(nameToQueue.get(matchmakerQueue.getName()))
                                                                .switchIfEmpty(Mono.defer(
                                                                    () -> getQueueFromApi(matchmakerQueue)))))
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .map(TupleUtils.function(matchmakerMapper::update))
                     .doOnError(throwable -> log.error("Error updating queue", throwable))
                     .retry()
                     .subscribe();

    fafServerAccessor.connectionStateProperty().subscribe(newValue -> {
      if (newValue == ConnectionState.CONNECTED) {
        sendFactions();
      }
    });

    party.ownerProperty().subscribe((oldValue, newValue) -> {
      if (oldValue != null) {
        chatService.leaveChannel("#" + oldValue.getUsername() + PARTY_CHANNEL_SUFFIX);
      }

      if (newValue != null) {
        chatService.joinChannel("#" + newValue.getUsername() + PARTY_CHANNEL_SUFFIX);
      }
    });

    matchmakerPrefs.getFactions().subscribe(this::sendFactions);

    partyMembersNotReady.bind(playersInGame.emptyProperty().map(empty -> !empty));
  }

  private void sendFactions() {
    sendFactionSelection(matchmakerPrefs.getFactions());
  }

  private void onSearchInfo(SearchInfo message) {
    MatchmakerQueueBean matchmakingQueue = nameToQueue.get(message.getQueueName());
    if (matchmakingQueue != null) {
      boolean searchStarted = message.getState().equals(MatchmakerState.START);
      matchmakingQueue.setMatchingStatus(searchStarted ? MatchingStatus.SEARCHING : null);

      if (isSearching() && Objects.equals(party.getOwner(), playerService.getCurrentPlayer())) {
        matchmakingQueue.setSelected(searchStarted);
      }
    }
  }

  private void onInQueueChange(Boolean newValue) {
    if (newValue) {
      searching.set(true);
      gameService.startSearchMatchmaker();
    }

    if (!newValue) {
      searching.set(false);
      if (!matchFoundAndWaitingForGameLaunch.get()) {
        gameService.stopSearchMatchmaker();
      }
    }
  }

  private void onSelectedQueueChange(Change<? extends MatchmakerQueueBean> change) {
    ObservableSet<Integer> unselectedQueueIds = matchmakerPrefs.getUnselectedQueueIds();
    while (change.next()) {
      if (change.wasRemoved()) {
        List<MatchmakerQueueBean> removed = List.copyOf(change.getRemoved());
        removed.stream().map(MatchmakerQueueBean::getId).forEach(unselectedQueueIds::add);
      }

      if (change.wasAdded()) {
        List<MatchmakerQueueBean> added = List.copyOf(change.getAddedSubList());
        added.stream().map(MatchmakerQueueBean::getId).forEach(unselectedQueueIds::remove);
      }
    }
  }

  private void onValidQueueChange(Change<? extends MatchmakerQueueBean> change) {
    if (!isSearching() || !Objects.equals(party.getOwner(), playerService.getCurrentPlayer())) {
      return;
    }

    while (change.next()) {
      if (change.wasRemoved()) {
        List<MatchmakerQueueBean> removed = List.copyOf(change.getRemoved());
        removed.stream()
               .filter(queue -> queue.getMatchingStatus() == MatchingStatus.SEARCHING)
               .forEach(this::leaveQueue);
      }

      if (change.wasAdded()) {
        List<MatchmakerQueueBean> added = List.copyOf(change.getAddedSubList());
        added.stream().filter(queue -> queue.getMatchingStatus() != MatchingStatus.SEARCHING).forEach(this::joinQueue);
      }
    }
  }

  private void sendInviteNotifications(PlayerBean player) {
    ActionCallback callback = event -> this.acceptPartyInvite(player);

    notificationService.addNotification(new TransientNotification(i18n.get("teammatchmaking.notification.invite.title"),
                                                                  i18n.get(
                                                                      "teammatchmaking.notification.invite.message",
                                                                      player.getUsername()),
                                                                  IdenticonUtil.createIdenticon(player.getId()),
                                                                  callback));
    notificationService.addNotification(
        new PersistentNotification(i18n.get("teammatchmaking.notification.invite.message", player.getUsername()),
                                   Severity.INFO, Collections.singletonList(
            new Action(i18n.get("teammatchmaking.notification.invite.join"), callback))));
  }

  private void changeLabelForQueues(MatchingStatus matchingStatus) {
    queues.stream()
          .filter(matchmakingQueue -> matchmakingQueue.getMatchingStatus() != null)
          .forEach(
              matchmakingQueue -> setTimedOutMatchingStatus(matchmakingQueue, matchingStatus, Duration.ofSeconds(60)));
  }

  private void setTimedOutMatchingStatus(MatchmakerQueueBean queue, MatchingStatus status, Duration timeout) {
    queue.setMatchingStatus(status);
    taskScheduler.schedule(() -> {
      if (queue.getMatchingStatus() == status) {
        queue.setMatchingStatus(null);
      }
    }, Instant.now().plus(timeout));
  }

  private void notifyMatchFound() {
    notificationService.addNotification(
        new TransientNotification(i18n.get("teammatchmaking.notification.matchFound.title"),
                                  i18n.get("teammatchmaking.notification.matchFound.message")));
  }

  private void setFoundStatusForQueue(MatchmakerMatchFoundResponse message) {
    MatchmakerQueueBean matchedQueue = nameToQueue.get(message.getQueueName());
    if (matchedQueue == null) {
      return;
    }

    setTimedOutMatchingStatus(matchedQueue, MatchingStatus.MATCH_FOUND, Duration.ofSeconds(60));
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

  private Mono<MatchmakerQueueBean> getQueueFromApi(MatchmakerInfo.MatchmakerQueue matchmakerQueue) {
    ElideNavigatorOnCollection<MatchmakerQueue> navigator = ElideNavigator.of(MatchmakerQueue.class)
                                                                          .collection()
                                                                          .setFilter(qBuilder().string("technicalName")
                                                                                               .eq(matchmakerQueue.getName()));
    return fafApiAccessor.getMany(navigator)
                         .next()
                         .map(dto -> matchmakerMapper.map(dto, new CycleAvoidingMappingContext()))
                         .map(queue -> matchmakerMapper.update(matchmakerQueue, queue))
                         .doOnNext(queue -> queue.setSelected(
                             !matchmakerPrefs.getUnselectedQueueIds().contains(queue.getId())))
                         .doOnNext(queue -> JavaFxUtil.addAndTriggerListener(gameService.getGames(),
                                                                             (SimpleInvalidationListener) () -> updateMatchmakerGameCount(
                                                                                 queue)))
                         .doOnNext(queue -> nameToQueue.put(queue.getTechnicalName(), queue));
  }

  public CompletableFuture<Boolean> joinQueues() {
    if (gamePathInvalid()) {
      return CompletableFuture.completedFuture(false);
    }

    if (gameService.isGameRunning()) {
      log.debug("Game is running, ignoring tmm queue join request");
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.gameAlreadyRunning.message");
      return CompletableFuture.completedFuture(false);
    }

    if (!Objects.equals(party.getOwner(), playerService.getCurrentPlayer())) {
      log.debug("Not party owner cannot join queues");
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.notPartyOwner.message");
      return CompletableFuture.completedFuture(false);
    }

    return modService.getFeaturedMod(FAF.getTechnicalName())
                     .toFuture()
                     .thenCompose(featuredModBean -> gameService.updateGameIfNecessary(featuredModBean, Set.of()))
                     .thenCompose(aVoid -> validQueues.stream()
                                                      .map(this::joinQueue)
                                                      .reduce((future1, future2) -> future1.thenCombine(future2,
                                                                                                        (result1, result2) -> result1 || result2))
                                                      .orElse(CompletableFuture.completedFuture(false)))
                     .exceptionally(throwable -> {
                       throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                       log.error("Unable to join queues", throwable);
                       if (throwable instanceof NotifiableException notifiableException) {
                         notificationService.addErrorNotification(notifiableException);
                       } else {
                         notificationService.addImmediateErrorNotification(throwable, "teammatchmaking.couldNotStart");
                       }
                       return false;
                     });
  }

  public void leaveQueues() {
    if (!Objects.equals(party.getOwner(), playerService.getCurrentPlayer())) {
      log.debug("Not party owner cannot join queues");
      notificationService.addImmediateWarnNotification("teammatchmaking.notification.notPartyOwner.message");
      return;
    }

    queues.stream().filter(queue -> queue.getMatchingStatus() == MatchingStatus.SEARCHING).forEach(this::leaveQueue);
  }

  private CompletableFuture<Boolean> joinQueue(MatchmakerQueueBean queue) {
    return mapService.downloadAllMatchmakerMaps(queue)
                     .thenRun(() -> fafServerAccessor.gameMatchmaking(queue, MatchmakerState.START))
                     .thenApply(aVoid -> true)
                     .exceptionally(throwable -> {
                       throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                       log.error("Unable to join queue `{}`", queue.getTechnicalName(), throwable);
                       notificationService.addImmediateErrorNotification(throwable, "teammatchmaking.couldNotJoinQueue",
                                                                         queue.getTechnicalName());
                       queue.setMatchingStatus(null);
                       return false;
                     });
  }

  private void leaveQueue(MatchmakerQueueBean queue) {
    fafServerAccessor.gameMatchmaking(queue, MatchmakerState.STOP);
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
    if (isInQueue() || isSearching()) {
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
    navigationHandler.navigateTo(new OpenTeamMatchmakingEvent());
  }

  public void invitePlayer(String player) {
    if (isInQueue() || isSearching()) {
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

    fxApplicationThreadExecutor.execute(() -> {
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
    });
  }

  public void sendReady(String requestId) {
    fafServerAccessor.sendReady(requestId);
  }

  public void unreadyParty() {
    fafServerAccessor.unreadyParty();
  }

  public void sendFactionSelection(List<Faction> factions) {
    fafServerAccessor.setPartyFactions(factions);
  }

  private boolean gamePathInvalid() {
    if (!preferencesService.isValidGamePath()) {
      missingGamePathNotifier.onMissingGamePathEvent(true);
      return true;
    }
    return false;
  }

  public boolean isInQueue() {
    return inQueue.get();
  }

  public ReadOnlyBooleanProperty inQueueProperty() {
    return inQueue.getReadOnlyProperty();
  }

  private synchronized void setPartyFromInfoMessage(PartyInfo message) {
    setOwnerFromInfoMessage(message);
    setMembersFromInfoMessage(message);
  }

  private synchronized void setOwnerFromInfoMessage(PartyInfo message) {
    playerService.getPlayerByIdIfOnline(message.getOwner()).ifPresent(player -> {
      party.setOwner(player);
    });
  }

  private synchronized void setMembersFromInfoMessage(PartyInfo message) {
    playersInGame.clear();
    List<PartyMember> members = message.getMembers()
                                       .stream()
                                       .map(this::createPartyMemberFromOnlinePLayers)
                                       .filter(Objects::nonNull)
                                       .peek(partyMember -> partyMember.setGameStatusChangeListener(
                                           observable -> checkMemberGameStatus(partyMember)))
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

  public boolean isAnyQueueSelected() {
    return anyQueueSelected.get();
  }

  public BooleanBinding anyQueueSelectedProperty() {
    return anyQueueSelected;
  }

  public boolean isSearching() {
    return searching.get();
  }

  public BooleanProperty searchingProperty() {
    return searching;
  }

  public void setSearching(boolean searching) {
    this.searching.set(searching);
  }

  public void requestMatchmakerInfo() {
    fafServerAccessor.requestMatchmakerInfo();
  }
}
