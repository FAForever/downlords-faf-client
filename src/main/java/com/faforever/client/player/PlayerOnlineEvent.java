package com.faforever.client.player;

import com.faforever.client.domain.server.PlayerInfo;

public record PlayerOnlineEvent(PlayerInfo player) {}
