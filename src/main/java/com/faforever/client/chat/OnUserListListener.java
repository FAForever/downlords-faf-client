package com.faforever.client.chat;

import java.util.Set;

public interface OnUserListListener {

  void onChatUserList(String channelName, Set<ChatUser> users);
}
