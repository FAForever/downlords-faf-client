package com.faforever.client.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatUserCategory {

  SELF("chat.category.self"),
  MODERATOR("chat.category.moderators"),
  FRIEND("chat.category.friends"),
  OTHER("chat.category.others"),
  CHAT_ONLY("chat.category.chatOnly"),
  FOE("chat.category.foes");

  private final String i18nKey;
}
