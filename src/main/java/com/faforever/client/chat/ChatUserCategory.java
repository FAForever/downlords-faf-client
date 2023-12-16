package com.faforever.client.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ChatUserCategory {
  // Order is important
  SELF("chat.category.self", "self"), MODERATOR("chat.category.moderators", "moderator"), FRIEND(
      "chat.category.friends", "friend"), OTHER("chat.category.others", "other"), CHAT_ONLY("chat.category.chatOnly",
                                                                                            "chat_only"), FOE(
      "chat.category.foes", "foe");

  private final String i18nKey;
  private final String cssClass;
}
