package com.faforever.client.teammatchmaking;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GameService;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.Action.ActionCallback;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.preferences.event.MissingGamePathEvent;
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
import com.faforever.client.teammatchmaking.event.PartyOwnerChangedEvent;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.commons.api.dto.Faction;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TeamMatchmakingService implements InitializingBean {

  private final PlayerService playerService;
  private final NotificationService notificationService;
  private final PreferencesService preferencesService;
  private final FafService fafService;
  private final EventBus eventBus;
  private final I18n i18n;
  private final TaskScheduler taskScheduler;
  private final GameService gameService;

  @Getter
  private final Party party;
  @Getter
  private final ObservableList<MatchmakingQueue> matchmakingQueues = FXCollections.observableArrayList();
  private final ObservableMap<Integer, MatchmakingQueue> queueIdToQueue = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  private final ReadOnlyBooleanWrapper partyMembersNotReady;
  private final ObservableSet<Player> playersInGame;
  private final List<ScheduledFuture<?>> leaveQueueTimeouts = new LinkedList<>();

  private volatile boolean matchFoundAndWaitingForGameLaunch = false;
  private final BooleanProperty currentlyInQueue = new SimpleBooleanProperty();
  private final InvalidationListener queueJoinInvalidationListener;

  public TeamMatchmakingService(PlayerService playerService, NotificationService notificationService, PreferencesService preferencesService, FafService fafService, EventBus eventBus, I18n i18n, TaskScheduler taskScheduler, GameService gameService) {
    this.playerService = playerService;
    this.notificationService = notificationService;
    this.preferencesService = preferencesService;
    this.fafService = fafService;
    this.eventBus = eventBus;
    this.i18n = i18n;
    this.taskScheduler = taskScheduler;
    this.gameService = gameService;

    fafService.addOnMessageListener(PartyInviteMessage.class, this::onPartyInvite);
    fafService.addOnMessageListener(PartyKickedMessage.class, this::onPartyKicked);
    fafService.addOnMessageListener(UpdatePartyMessage.class, this::onPartyInfo);
    fafService.addOnMessageListener(SearchInfoMessage.class, this::onSearchInfoMessage);
    fafService.addOnMessageListener(MatchFoundMessage.class, this::onMatchFoundMessage);
    fafService.addOnMessageListener(MatchCancelledMessage.class, this::onMatchCancelledMessage);
    fafService.addOnMessageListener(GameLaunchMessage.class, this::onGameLaunchMessage);
    fafService.addOnMessageListener(MatchmakerInfoMessage.class, this::onMatchmakerInfo);

    party = new Party();

    playersInGame = FXCollections.observableSet();
    partyMembersNotReady = new ReadOnlyBooleanWrapper();

    partyMembersNotReady.bind(Bindings.createBooleanBinding(() -> !playersInGame.isEmpty(), playersInGame));

    queueJoinInvalidationListener = observable -> currentlyInQueue.set(matchmakingQueues.stream().anyMatch(MatchmakingQueue::isJoined));

    JavaFxUtil.attachListToMap(matchmakingQueues, queueIdToQueue);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  @VisibleForTesting
  protected void onMatchmakerInfo(MatchmakerInfoMessage message) {
    message.getQueues().forEach(this::updateOrCreateQueue);
  }

  private synchronized void updateOrCreateQueue(MatchmakerQueue messageQueue) {
    matchmakingQueues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), messageQueue.getQueueName()))
        .findFirst()
        .ifPresentOrElse(matchmakingQueue -> updateQueueInfo(matchmakingQueue, messageQueue),
            () -> fafService.getMatchmakingQueue(messageQueue.getQueueName()).thenAccept(matchmakingQueueFromApi ->
                matchmakingQueueFromApi.ifPresent(apiQueue -> {
                  queueIdToQueue.put(apiQueue.getQueueId(), apiQueue);
                  apiQueue.joinedProperty().addListener(queueJoinInvalidationListener);
                  updateQueueInfo(apiQueue, messageQueue);
                })));
  }

  private void updateQueueInfo(MatchmakingQueue queue, MatchmakerQueue messageQueue) {
    queue.setQueuePopTime(OffsetDateTime.parse(messageQueue.getQueuePopTime()).toInstant());
    queue.setTeamSize(messageQueue.getTeamSize());
    queue.setPartiesInQueue(messageQueue.getBoundary75s().size());
    queue.setPlayersInQueue(messageQueue.getNumPlayers());
  }

  @VisibleForTesting
  protected void onSearchInfoMessage(SearchInfoMessage message) {
    matchmakingQueues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), message.getQueueName()))
        .forEach(matchmakingQueue -> {
          matchmakingQueue.setJoined(message.getState() == MatchmakingState.START);
          leaveQueueTimeouts.forEach(f -> f.cancel(false));

              if (message.getState() == MatchmakingState.START) {
                gameService.startSearchMatchmaker();

                party.getMembers().stream()
                    .filter(partyMember -> Objects.equals(partyMember.getPlayer(), playerService.getCurrentPlayer()))
                    .findFirst()
                    .ifPresent(member -> sendFactionSelection(member.getFactions()));
              }
            }
        );

    if (matchmakingQueues.stream().noneMatch(MatchmakingQueue::isJoined) && !matchFoundAndWaitingForGameLaunch) {
      gameService.onMatchmakerSearchStopped();
    }
  }

  @VisibleForTesting
  protected void onMatchFoundMessage(MatchFoundMessage message) {
    matchFoundAndWaitingForGameLaunch = true; // messages from server: match found -> STOP all queues that you are in that haven't found a match -> game launch

    notificationService.addNotification(new TransientNotification(
        i18n.get("teammatchmaking.notification.matchFound.title"),
        i18n.get("teammatchmaking.notification.matchFound.message")
    ));

    matchmakingQueues.stream()
        .filter(matchmakingQueue -> Objects.equals(matchmakingQueue.getTechnicalName(), message.getQueueName()))
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(MatchingStatus.MATCH_FOUND, Duration.ofSeconds(60), taskScheduler));

    matchmakingQueues.forEach(matchmakingQueue -> matchmakingQueue.setJoined(false));
  }

  @VisibleForTesting
  protected void onMatchCancelledMessage(MatchCancelledMessage message) {
    matchmakingQueues.stream()
        .filter(matchmakingQueue -> matchmakingQueue.getMatchingStatus() != null)
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(MatchingStatus.MATCH_CANCELLED, Duration.ofSeconds(60), taskScheduler));

    matchFoundAndWaitingForGameLaunch = false;
    gameService.onMatchmakerSearchStopped(); // joining custom games is still blocked till match is cancelled or launched
  }

  @VisibleForTesting
  protected void onGameLaunchMessage(GameLaunchMessage message) {
    if (message.getInitMode() != LobbyMode.AUTO_LOBBY) {
      return;
    }

    matchmakingQueues.stream()
        .filter(matchmakingQueue -> matchmakingQueue.getMatchingStatus() != null)
        .forEach(matchmakingQueue -> matchmakingQueue.setTimedOutMatchingStatus(MatchingStatus.GAME_LAUNCHING, Duration.ofSeconds(60), taskScheduler));

    matchFoundAndWaitingForGameLaunch = false;
    gameService.onMatchmakerSearchStopped(); // joining custom games is still blocked till match is cancelled or launched
  }

  public boolean joinQueue(MatchmakingQueue queue) {
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

    fafService.updateMatchmakerState(queue, MatchmakingState.START);
    return true;
  }

  public void leaveQueue(MatchmakingQueue queue) {
    fafService.updateMatchmakerState(queue, MatchmakingState.STOP);
    leaveQueueTimeouts.add(taskScheduler.schedule(
        () -> JavaFxUtil.runLater(() -> queue.setJoined(false)), Instant.now().plus(Duration.ofSeconds(5))));
  }

  public void onPartyInfo(UpdatePartyMessage message) {
    Player currentPlayer = playerService.getCurrentPlayer();
    if (message.getMembers().stream().noneMatch(partyMember -> partyMember.getPlayer() == currentPlayer.getId())) {
      initializeParty();
      return;
    }
    setPartyFromInfoMessage(message);
  }

  public void onPartyInvite(PartyInviteMessage message) {
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
  protected void acceptPartyInvite(Player player) {
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

    fafService.acceptPartyInvite(player);
    eventBus.post(new OpenTeamMatchmakingEvent());
  }

  public void onPartyKicked(PartyKickedMessage message) {
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

    playerService.getPlayerByNameIfOnline(player).ifPresent(fafService::inviteToParty);
  }

  public void kickPlayerFromParty(Player player) {
    fafService.kickPlayerFromParty(player);
    playersInGame.remove(player);
  }

  public void leaveParty() {
    fafService.leaveParty();
    initializeParty();
  }

  private void initializeParty() {
    Player currentPlayer = playerService.getCurrentPlayer();

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
    fafService.readyParty();
  }

  public void unreadyParty() {
    fafService.unreadyParty();
  }

  public void sendFactionSelection(List<Faction> factions) {
    fafService.setPartyFactions(factions);
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

  private synchronized void setPartyFromInfoMessage(UpdatePartyMessage message) {
    setOwnerFromInfoMessage(message);
    setMembersFromInfoMessage(message);
  }

  private synchronized void setOwnerFromInfoMessage(UpdatePartyMessage message) {
    playerService.getPlayerByIdIfOnline(message.getOwner()).ifPresent(player -> {
      party.setOwner(player);
      eventBus.post(new PartyOwnerChangedEvent(player));
    });
  }

  private synchronized void setMembersFromInfoMessage(UpdatePartyMessage message) {
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
    Player memberPlayer = partyMember.getPlayer();
    if (memberPlayer.getStatus() != PlayerStatus.IDLE) {
      playersInGame.add(memberPlayer);
    } else {
      playersInGame.remove(memberPlayer);
    }
  }

  private PartyMember createPartyMemberFromOnlinePLayers(UpdatePartyMessage.PartyMember member) {
    return playerService.getPlayerByIdIfOnline(member.getPlayer())
        .map(player -> new PartyMember(player, member.getFactions()))
        .orElseGet(() -> {
          log.warn("Could not find party member {}", member.getPlayer());
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
    fafService.requestMatchmakerInfo();
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
