package com.faforever.client.chat;

import java.util.Set;

public interface OnUserListListener {

  void onUserList(String channelName, Set<ChatUser> users);
}
