package com.faforever.client.remote;

import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.ServerMessage;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Entry class for all communication with the FAF server.
 */
public interface FafServerAccessor {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletionStage<LoginMessage> connectAndLogIn(String username, String password);

  CompletionStage<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo);

  CompletionStage<GameLaunchMessage> requestJoinGame(int gameId, String password);

  void disconnect();

  void reconnect();

  void addFriend(int playerId);

  void addFoe(int playerId);

  CompletionStage<GameLaunchMessage> startSearchRanked1v1(Faction faction);

  void stopSearchingRanked();

  @Nullable
  Long getSessionId();

  void sendGpgMessage(GpgGameMessage message);

  void removeFriend(int playerId);

  void removeFoe(int playerId);

  void selectAvatar(URL url);

  List<Avatar> getAvailableAvatars();

}
