package com.faforever.client.remote;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.IceServer;
import com.faforever.commons.lobby.LobbyMode;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.Player.Avatar;
import com.faforever.commons.lobby.ServerMessage;
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
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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

  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;

  private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void addEventListener(Class<T> type, Consumer<T> listener) {

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
  public CompletableFuture<LoginSuccessResponse> connectAndLogIn() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<GameLaunchResponse> requestHostGame(NewGameInfo newGameInfo) {
    return taskService.submitTask(new CompletableTask<GameLaunchResponse>(HIGH) {
      @Override
      protected GameLaunchResponse call() throws Exception {
        updateTitle("Hosting game");

        return new GameLaunchResponse(1234, "", "faf", LobbyMode.DEFAULT_LOBBY, "",
            List.of("/ratingcolor d8d8d8d8", "/numgames 1234"), "", null,
             null, null, null);
      }
    }).getFuture();
  }

  @Override
  public CompletableFuture<GameLaunchResponse> requestJoinGame(int gameId, String password) {
    return taskService.submitTask(new CompletableTask<GameLaunchResponse>(HIGH) {
      @Override
      protected GameLaunchResponse call() throws Exception {
        updateTitle("Joining game");

        return new GameLaunchResponse(1234, "", "faf", LobbyMode.DEFAULT_LOBBY, "",
            List.of("/ratingcolor d8d8d8d8", "/numgames 1234"), "", null,
            null, null, null);
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
  public CompletableFuture<GameLaunchResponse> startSearchMatchmaker() {
    log.debug("Starting matchmaker game");
    GameLaunchResponse gameLaunchMessage = new GameLaunchResponse(1234, "", "faf", LobbyMode.DEFAULT_LOBBY, "",
        List.of("/ratingcolor d8d8d8d8", "/numgames 1234"), "", null,
        null, null, null);
    return CompletableFuture.completedFuture(gameLaunchMessage);
  }

  @Override
  public void sendGpgMessage(GpgGameOutboundMessage message) {

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
  public void closePlayersGame(int playerId) {

  }

  @Override
  public void closePlayersLobby(int playerId) {

  }

  @Override
  public void broadcastMessage(String message) {

  }

  @Override
  public CompletableFuture<Collection<Avatar>> getAvailableAvatars() {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public CompletableFuture<Collection<IceServer>> getIceServers() {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public void restoreGameSession(int id) {

  }

  @Override
  public void gameMatchmaking(MatchmakingQueue queue, MatchmakerState state) {

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
}
