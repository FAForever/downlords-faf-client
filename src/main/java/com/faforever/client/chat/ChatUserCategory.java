package com.faforever.client.chat;

enum ChatUserCategory {
  MODERATOR("chat.category.moderators"),
  FRIEND("chat.category.friends"),
  OTHER("chat.category.others"),
  CHAT_ONLY("chat.category.chatOnly"),
  FOE("chat.category.foes");

  private final String i18nKey;

  ChatUserCategory(String i18nKey) {
    this.i18nKey = i18nKey;
  }

  public String getI18nKey() {
    return i18nKey;
  }
}
