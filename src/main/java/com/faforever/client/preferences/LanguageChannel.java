package com.faforever.client.preferences;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LanguageChannel {
  FRENCH("#french"),
  GERMAN("#german"),
  RUSSIAN("#russian");

  private final String channelName;
}
