package com.faforever.client.player.event;

import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;

public record FriendJoinedGameEvent(PlayerInfo player, GameInfo game) {}
