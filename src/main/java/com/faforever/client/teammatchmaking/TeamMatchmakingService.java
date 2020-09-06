package com.faforever.client.teammatchmaking;

import com.faforever.client.fa.relay.LobbyMode;
import com.faforever.client.game.Faction;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenTeamMatchmakingEvent;
import com.faforever.client.net.ConnectionState;
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
import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.remote.FafServerAccessor;
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
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Lazy
@Service
@Slf4j
public class TeamMatchmakingService implements InitializingBean {

  private final FafServerAccessor fafServerAccessor;
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
  private final List<ScheduledFuture<?>> leaveQueueTimeouts = new LinkedList<>();

  @Override
  public void afterPropertiesSet() throws Exception {

  }

  public TeamMatchmakingService(FafServerAccessor fafServerAccessor, PlayerService playerService, NotificationService notificationService, PreferencesService preferencesService, FafService fafService, EventBus eventBus, I18n i18n, TaskScheduler taskScheduler, GameService gameService) {
    this.fafServerAccessor = fafServerAccessor;
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
    fafService.addOnMessageListener(PartyInfoMessage.class, this::onPartyInfo);
    fafService.addOnMessageListener(SearchInfoMessage.class, this::onSearchInfoMessage);
    fafService.addOnMessageListener(MatchFoundMessage.class, this::onMatchFoundMessage);
    fafService.addOnMessageListener(MatchCancelledMessage.class, this::onMatchCancelledMessage);
    fafService.addOnMessageListener(GameLaunchMessage.class, this::onGameLaunchMessage);
    fafService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.DISCONNECTED) {
        Platform.runLater(() -> initParty(playerService.getCurrentPlayer().get()));
      }
    });

    fafService.addOnMessageListener(MatchmakerInfoMessage.class, this::onMatchmakerInfo);

    party = new Party();

    playerService.currentPlayerProperty().addListener((obs, old, player) -> {
      if (party.getOwner() == null && party.getMembers().isEmpty() && player != null) {
        Platform.runLater(() -> initParty(player));
      }
    });

    playerService.getCurrentPlayer().ifPresent(this::initParty);
  }

  private void onMatchmakerInfo(MatchmakerInfoMessage message) {
    Platform.runLater(() -> {
      message.getQueues().forEach(remoteQueue -> {
        MatchmakingQueue localQueue = matchmakingQueues.stream()
            .filter(q -> Objects.equals(q.getQueueName(), remoteQueue.getQueueName())).findFirst().orElse(null);

        if (localQueue == null) {
          localQueue = new MatchmakingQueue(remoteQueue.getQueueName());
          matchmakingQueues.add(localQueue);
        }

        localQueue.setQueuePopTime(OffsetDateTime.parse(remoteQueue.getQueuePopTime()).toInstant());
        localQueue.setTeamSize(remoteQueue.getTeamSize());
        localQueue.setPartiesInQueue(remoteQueue.getBoundary75s().size());
        localQueue.setPlayersInQueue(remoteQueue.getNumPlayers());
      });
    });
  }

  private void onSearchInfoMessage(SearchInfoMessage message) {
    matchmakingQueues.stream().filter(q -> Objects.equals(q.getQueueName(), message.getQueueName())).forEach(q -> {
          Platform.runLater(() -> {
            q.setJoined(message.getState() == MatchmakingState.START);
            leaveQueueTimeouts.forEach(f -> f.cancel(false));
          });

          //TODO: check current state / other queues
          if (message.getState() == MatchmakingState.START) {
            gameService.startSearchMatchmakerTodo();
          }
        }
    );
  }

  private void onMatchFoundMessage(MatchFoundMessage message) {
    notificationService.addNotification(new TransientNotification(
        i18n.get("teammatchmaking.notification.matchFound.title"),
        i18n.get("teammatchmaking.notification.matchFound.message")
    ));
    matchmakingQueues.stream().filter(q -> Objects.equals(q.getQueueName(), message.getQueue())).forEach(q -> {
      q.setTimedOutMatchingStatus(MatchingStatus.MATCH_FOUND, Duration.ofSeconds(15), taskScheduler);
    });
  }

  private void onMatchCancelledMessage(MatchCancelledMessage message) {
    matchmakingQueues.stream().filter(q -> q.getMatchingStatus() != null).forEach(q -> {
      q.setTimedOutMatchingStatus(MatchingStatus.MATCH_CANCELLED, Duration.ofSeconds(15), taskScheduler);
    });
  }

  private void onGameLaunchMessage(GameLaunchMessage message) {
    if (message.getInitMode() != LobbyMode.AUTO_LOBBY) {
      return;
    }

    matchmakingQueues.stream().filter(q -> q.getMatchingStatus() != null).forEach(q -> {
      q.setTimedOutMatchingStatus(MatchingStatus.GAME_LAUNCHING, Duration.ofSeconds(15), taskScheduler);
    });
  }

  public void joinQueue(MatchmakingQueue queue) {
    if (!ensureValidGamePath()) {
      return;
    }
    fafServerAccessor.gameMatchmaking(queue, MatchmakingState.START);
  }

  public void leaveQueue(MatchmakingQueue queue) {
    fafServerAccessor.gameMatchmaking(queue, MatchmakingState.STOP);

    leaveQueueTimeouts.add(taskScheduler.schedule(
        () -> Platform.runLater(() -> queue.setJoined(false)), Instant.now().plus(Duration.ofSeconds(5))));
  }

  public void onPartyInfo(PartyInfoMessage message) {
    Optional<Player> currentPlayer = playerService.getCurrentPlayer();
    if (currentPlayer.isPresent()) {
      if (message.getMembers().stream().noneMatch(m -> m.getPlayer() == currentPlayer.get().getId())) {
        Platform.runLater(() -> initParty(currentPlayer.get()));
        return;
      }
    }
    party.fromInfoMessage(message, playerService);
  }

  public void onPartyInvite(PartyInviteMessage message) {
    playerService.getPlayersByIds(Collections.singletonList(message.getSender()))
        .thenAccept(players -> {
          if (players.isEmpty()) {
            log.error("User {} of party invite not found", message.getSender());
            return;
          }
          Player player = players.get(0);
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
        });
  }

  private void acceptPartyInvite(Player player) {
    if (isCurrentlyInQueue()) {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("teammatchmaking.notification.joinAlreadyInQueue.title"),
          i18n.get("teammatchmaking.notification.joinAlreadyInQueue.message"),
          Severity.WARN,
          Collections.singletonList(new Action(i18n.get("dismiss")))
      ));
      return;
    }

    if (!ensureValidGamePath()) {
      return;
    }

    fafServerAccessor.acceptPartyInvite(player); // TODO: SHOULD ALL OF THE fafServerAccessor calls be move to fafService????
    eventBus.post(new OpenTeamMatchmakingEvent());
  }

  public void onPartyKicked(PartyKickedMessage message) {
    Platform.runLater(() -> initParty(playerService.getCurrentPlayer().get()));
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

    if (!ensureValidGamePath()) {
      return;
    }

    playerService.getPlayerForUsername(player).ifPresent(fafServerAccessor::inviteToParty);
  }

  public void kickPlayerFromParty(Player player) {
    fafServerAccessor.kickPlayerFromParty(player);
  }

  public void leaveParty() {
    fafServerAccessor.leaveParty();
    initParty(playerService.getCurrentPlayer().get());
  }

  private void initParty(Player owner) {
    party.setOwner(owner);
    party.getMembers().setAll(new PartyMember(owner));
  }

  public void readyParty() {
    fafServerAccessor.readyParty();
  }

  public void unreadyParty() {
    fafServerAccessor.unreadyParty();
  }

  public void setPartyFactions(List<Faction> factions) {
    fafServerAccessor.setPartyFactions(factions);
  }

  private boolean ensureValidGamePath() {
    if (!preferencesService.isGamePathValid()) {
      eventBus.post(new MissingGamePathEvent(true));
      return false;
    }
    return true;
  }

  public boolean isCurrentlyInQueue() {
    return matchmakingQueues.stream().anyMatch(MatchmakingQueue::isJoined);
  }
}
