package com.faforever.client.chat;

import java.util.Collection;
import java.util.Map;

public interface OnChatUserListListener {

  void onChatUserList(String channelName, Map<String, ChatUser> users);
}
