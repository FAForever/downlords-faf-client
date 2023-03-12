package com.faforever.client.player.event;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;

public record FriendJoinedGameEvent(PlayerBean player, GameBean game) {}
