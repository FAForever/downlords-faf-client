package com.faforever.client.chat;

import java.util.Map;

public interface OnUserListListener {

  void onChatUserList(String channelName, Map<String, PlayerInfoBean> users);
}
