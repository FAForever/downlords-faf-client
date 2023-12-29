package com.faforever.client.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ChatUserCategory {
  // Order here determines the order they show up in the chat list
  SELF("chat.category.self"),
  MODERATOR("chat.category.moderators"),
  FRIEND("chat.category.friends"),
  OTHER("chat.category.others"),
  FOE("chat.category.foes"),
  CHAT_ONLY("chat.category.chatOnly"),
  AWAY("chat.category.away");

  private final String i18nKey;
}
