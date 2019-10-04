package com.faforever.client.preferences;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum LanguageChannel {
  FRENCH("#french"),
  GERMAN("#german"),
  RUSSIAN("#russian");

  LanguageChannel(String channelName) {
    this.channelName = channelName;
  }

  private final String channelName;
}
