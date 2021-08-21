package com.faforever.client.player;

import com.faforever.client.domain.PlayerBean;
import lombok.Value;

@Value
public class PlayerOnlineEvent {
  PlayerBean player;
}
