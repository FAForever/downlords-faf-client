package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.GameStatus;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface PlayerService {

  void updatePlayerGameStatus(PlayerInfoBean playerInfoBean, GameStatus gameStatus);

  /**
   * Returns the PlayerInfoBean for the specified username. Returns null if no such player is known.
   */
  @Nullable
  PlayerInfoBean getPlayerForUsername(@Nullable String username);

  /**
   * Gets a player for the given username. A new user is created and registered if it does not yet exist.
   */
  PlayerInfoBean createAndGetPlayerForUsername(@NotNull String username);

  Set<String> getPlayerNames();

  void addFriend(PlayerInfoBean player);

  void removeFriend(PlayerInfoBean user);

  void addFoe(PlayerInfoBean username);

  void removeFoe(PlayerInfoBean player);

  PlayerInfoBean getCurrentPlayer();

  ReadOnlyObjectProperty<PlayerInfoBean> currentPlayerProperty();

}
