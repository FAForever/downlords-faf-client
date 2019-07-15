package com.faforever.client.chat;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;

public enum OfficialLanguageChannel {
  FRENCH("#french", "fr"),
  GERMAN("#german", "de"),
  RUSSIAN("#russian", "ru", "by");

  @VisibleForTesting
  public final String channelName;
  @VisibleForTesting
  public final String[] localeLanguages;
  

  OfficialLanguageChannel(String channelName, String... localeLanguages) {
    this.localeLanguages = localeLanguages;
    this.channelName = channelName;
  }

  public static Optional<String> getChannelName(String localeLanguage) {
    for (OfficialLanguageChannel channel : values()) {
      for (String localLanguage : channel.localeLanguages) {
        if (localLanguage.equals(localeLanguage)) {
          return Optional.of(channel.channelName);
        }
      }
    }
    return Optional.empty();
  }
}
