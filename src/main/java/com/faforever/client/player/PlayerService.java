package com.faforever.client.player;

import javafx.beans.property.ReadOnlyObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface PlayerService {

  /**
   * Returns the PlayerInfoBean for the specified username. Returns null if no such player is known.
   */
  @Nullable
  // TODO Use Optional
  Player getPlayerForUsername(@Nullable String username);

  /**
   * Gets a player for the given username. A new user is created and registered if it does not yet exist.
   */
  Player createAndGetPlayerForUsername(@NotNull String username);

  Set<String> getPlayerNames();

  void addFriend(Player player);

  void removeFriend(Player user);

  void addFoe(Player username);

  void removeFoe(Player player);

  Optional<Player> getCurrentPlayer();

  ReadOnlyObjectProperty<Player> currentPlayerProperty();

  CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerId);
}
