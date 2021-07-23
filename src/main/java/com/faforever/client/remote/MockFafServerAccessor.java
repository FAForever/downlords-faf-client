package com.faforever.client.remote;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.GameVisibility;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.GameType;
import com.faforever.client.remote.domain.LobbyMode;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.PlayerInfo;
import com.faforever.client.remote.domain.inbound.InboundMessage;
import com.faforever.client.remote.domain.inbound.faf.GameInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage.IceServer;
import com.faforever.client.remote.domain.inbound.faf.LoginMessage;
import com.faforever.client.remote.domain.inbound.faf.PlayerInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.UpdatedAchievementsMessage;
import com.faforever.client.remote.domain.outbound.gpg.GpgOutboundMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.api.dto.Faction;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.faforever.client.remote.domain.GameAccess.PASSWORD;
import static com.faforever.client.remote.domain.GameAccess.PUBLIC;
import static com.faforever.client.task.CompletableTask.Priority.HIGH;

@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
@Slf4j
// NOSONAR
public class MockFafServerAccessor implements FafServerAccessor {

  private static final String USER_NAME = "MockUser";
  private final Timer timer = new Timer("LobbyServerAccessorTimer", true);
  private final HashMap<Class<? extends InboundMessage>, Collection<Consumer<InboundMessage>>> messageListeners = new HashMap<>();

  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;

  private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

  @Override
  @SuppressWarnings("unchecked")
  public <T extends InboundMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    if (!messageListeners.containsKey(type)) {
      messageListeners.put(type, new LinkedList<>());
    }
    messageListeners.get(type).add((Consumer<InboundMessage>) listener);
  }

  @Override
  public <T extends InboundMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.get(type).remove(listener);
  }

  @Override
  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogin(String token) {
    return taskService.submitTask(new CompletableTask<LoginMessage>(HIGH) {
      @Override
      protected LoginMessage call() throws Exception {
        updateTitle(i18n.get("login.progress.message"));

        PlayerInfo playerInfo = new PlayerInfo(4812, USER_NAME, null, null, null, new HashMap<>(), new HashMap<>());

        PlayerInfoMessage playerInfoMessage = new PlayerInfoMessage(List.of(playerInfo));

        eventBus.post(new LoginSuccessEvent());

        messageListeners.getOrDefault(playerInfoMessage.getClass(), List.of()).forEach(consumer -> consumer.accept(playerInfoMessage));

        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            UpdatedAchievement updatedAchievement = new UpdatedAchievement();
            updatedAchievement.setAchievementId("50260d04-90ff-45c8-816b-4ad8d7b97ecd");
            updatedAchievement.setNewlyUnlocked(true);
            UpdatedAchievementsMessage updatedAchievementsMessage = new UpdatedAchievementsMessage(List.of(updatedAchievement));

            messageListeners.getOrDefault(updatedAchievementsMessage.getClass(), List.of()).forEach(consumer -> consumer.accept(updatedAchievementsMessage));
          }
        }, 7000);

        List<GameInfoMessage> gameInfoMessages = Arrays.asList(
            createGameInfo(1, "Mock game 500 - 800", PUBLIC, "faf", "scmp_010", 1, 6, "Mock user"),
            createGameInfo(2, "Mock game 500+", PUBLIC, "faf", "scmp_011", 2, 6, "Mock user"),
            createGameInfo(3, "Mock game +500", PUBLIC, "faf", "scmp_012", 3, 6, "Mock user"),
            createGameInfo(4, "Mock game <1000", PUBLIC, "faf", "scmp_013", 4, 6, "Mock user"),
            createGameInfo(5, "Mock game >1000", PUBLIC, "faf", "scmp_014", 5, 6, "Mock user"),
            createGameInfo(6, "Mock game ~600", PASSWORD, "faf", "scmp_015", 6, 6, "Mock user"),
            createGameInfo(7, "Mock game 7", PASSWORD, "faf", "scmp_016", 7, 6, "Mock user")
        );

        gameInfoMessages.forEach(gameInfoMessage ->
            messageListeners.getOrDefault(gameInfoMessage.getClass(), List.of())
                .forEach(consumer -> consumer.accept(gameInfoMessage)));

        notificationService.addNotification(
            new PersistentNotification(
                "How about a long-running (7s) mock task?",
                Severity.INFO,
                Arrays.asList(
                    new Action("Execute", event ->
                        taskService.submitTask(new CompletableTask<Void>(HIGH) {
                          @Override
                          protected Void call() throws Exception {
                            updateTitle("Mock task");
                            Thread.sleep(2000);
                            for (int i = 0; i < 5; i++) {
                              updateProgress(i, 5);
                              Thread.sleep(1000);
                            }
                            return null;
                          }
                        })),
                    new Action("Nope")
                )
            )
        );

        PlayerInfo me = new PlayerInfo(123, USER_NAME, null, null, null, new HashMap<>(), new HashMap<>());
        return new LoginMessage(me);
      }
    }).getFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return taskService.submitTask(new CompletableTask<GameLaunchMessage>(HIGH) {
      @Override
      protected GameLaunchMessage call() throws Exception {
        updateTitle("Hosting game");

        return new GameLaunchMessage(List.of("/ratingcolor d8d8d8d8", "/numgames 1234"), 1234, "faf",
            "", "", 0, 0, null,
            null, LobbyMode.DEFAULT_LOBBY, "");
      }
    }).getFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return taskService.submitTask(new CompletableTask<GameLaunchMessage>(HIGH) {
      @Override
      protected GameLaunchMessage call() throws Exception {
        updateTitle("Joining game");

        return new GameLaunchMessage(List.of("/ratingcolor d8d8d8d8", "/numgames 1234"), 1234, "faf",
            "", "", 0, 0, null,
            null, LobbyMode.DEFAULT_LOBBY, "");
      }
    }).getFuture();
  }

  @Override
  public void disconnect() {

  }

  @Override
  public void reconnect() {

  }

  @Override
  public void addFriend(int playerId) {

  }

  @Override
  public void addFoe(int playerId) {

  }

  @Override
  public void requestMatchmakerInfo() {

  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchMatchmaker() {
    log.debug("Starting matchmaker game");
    GameLaunchMessage gameLaunchMessage = new GameLaunchMessage(List.of("/ratingcolor d8d8d8d8", "/numgames 1234"), 1234, "faf",
        "", "", 0, 0, null,
        null, LobbyMode.DEFAULT_LOBBY, "");
    return CompletableFuture.completedFuture(gameLaunchMessage);
  }

  @Override
  public void stopSearchMatchmaker() {

  }

  @Override
  public void sendGpgMessage(GpgOutboundMessage message) {

  }

  @Override
  public void removeFriend(int playerId) {

  }

  @Override
  public void removeFoe(int playerId) {

  }

  @Override
  public void selectAvatar(URL url) {

  }

  @Override
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {

  }

  @Override
  public void closePlayersGame(int playerId) {

  }

  @Override
  public void closePlayersLobby(int playerId) {

  }

  @Override
  public void broadcastMessage(String message) {

  }

  @Override
  public List<Avatar> getAvailableAvatars() {
    return List.of();
  }

  @Override
  public CompletableFuture<List<IceServer>> getIceServers() {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public void restoreGameSession(int id) {

  }

  @Override
  public void ping() {

  }

  @Override
  public void gameMatchmaking(MatchmakingQueue queue, MatchmakingState state) {

  }

  @Override
  public void inviteToParty(com.faforever.client.player.Player recipient) {

  }

  @Override
  public void acceptPartyInvite(com.faforever.client.player.Player sender) {

  }

  @Override
  public void kickPlayerFromParty(com.faforever.client.player.Player kickedPlayer) {

  }

  @Override
  public void readyParty() {

  }

  @Override
  public void unreadyParty() {

  }

  @Override
  public void leaveParty() {

  }

  @Override
  public void setPartyFactions(List<Faction> factions) {

  }


  private GameInfoMessage createGameInfo(int uid, String title, GameAccess access, String featuredMod, String mapName, int numPlayers, int maxPlayers, String host) {
    return new GameInfoMessage(host, false, GameVisibility.PUBLIC, GameStatus.OPEN, numPlayers, Map.of(),
        featuredMod, uid, maxPlayers, "", Map.of(), "", 0.0, "", null, null, false,
        GameType.CUSTOM, List.of());
  }
}
