package com.faforever.client.chat;

public enum ChatColorMode {
  DEFAULT("settings.chat.colorMode.default"),
  RANDOM("settings.chat.colorMode.random");

  private final String i18nKey;

  ChatColorMode(String i18nKey) {
    this.i18nKey = i18nKey;
  }

  public String getI18nKey() {
    return i18nKey;
  }

}
