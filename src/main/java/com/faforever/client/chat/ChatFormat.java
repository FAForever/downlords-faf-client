package com.faforever.client.chat;

import lombok.Getter;

public enum ChatFormat {
  EXTENDED("settings.chat.chatFormat.extended"),
  COMPACT("settings.chat.chatFormat.compact");

  @Getter
  private final String i18nKey;

  ChatFormat(String i18nKey) {
    this.i18nKey = i18nKey;
  }
}
