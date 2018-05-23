package com.faforever.client.chat;

import java.util.Optional;

public enum OfficialLanguageChannel {
  FRENCH("fr", "#french"),
  GERMAN("de", "#german"),
  RUSSIAN("ru", "#russian");

  private final String localeLanguage;
  private final String channelName;


  OfficialLanguageChannel(String localeLanguage, String channelName) {
    this.localeLanguage = localeLanguage;
    this.channelName = channelName;
  }

  public static Optional<OfficialLanguageChannel> getChannelName(String localeLanguage) {
    for (OfficialLanguageChannel channel : values()) {
      if (channel.getLocaleLanguage().equals(localeLanguage)) {
        return Optional.of(channel);
      }
    }
    return Optional.empty();
  }

  public String getChannelName() {
    return channelName;
  }

  public String getLocaleLanguage() {
    return localeLanguage;
  }
}
