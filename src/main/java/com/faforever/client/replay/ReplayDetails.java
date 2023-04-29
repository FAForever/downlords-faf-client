package com.faforever.client.replay;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean.ChatMessage;
import com.faforever.client.domain.ReplayBean.GameOption;

import java.util.List;

public record ReplayDetails(List<ChatMessage> chatMessages, List<GameOption> gameOptions, MapVersionBean mapVersion) {

  public ReplayDetails {
    chatMessages = List.copyOf(chatMessages);
    gameOptions = List.copyOf(gameOptions);
  }
}
