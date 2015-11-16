package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface PlayerService {

  /**
   * Returns the PlayerInfoBean for the specified username. Returns null if no such player is known.
   */
  PlayerInfoBean getPlayerForUsername(@Nullable String username);

  /**
   * Gets a player for the given username. A new user is created and registered if it does not yet exist.
   */
  PlayerInfoBean registerAndGetPlayerForUsername(@NotNull String username);

  Set<String> getPlayerNames();

  void addFriend(String username);

  void removeFriend(String username);

  void addFoe(String username);

  void removeFoe(String username);

  PlayerInfoBean getCurrentPlayer();

  ReadOnlyObjectProperty<PlayerInfoBean> currentPlayerProperty();
}
