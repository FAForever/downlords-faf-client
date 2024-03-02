package com.faforever.client.replay;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.Replay.ChatMessage;
import com.faforever.client.domain.api.Replay.GameOption;

import java.util.List;

public record ReplayDetails(List<ChatMessage> chatMessages, List<GameOption> gameOptions, MapVersion mapVersion) {

  public ReplayDetails {
    chatMessages = List.copyOf(chatMessages);
    gameOptions = List.copyOf(gameOptions);
  }
}
