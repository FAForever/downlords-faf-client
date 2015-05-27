package com.faforever.client.chat;

public interface OnChannelJoinedListener {

  void onUserJoinedChannel(String channelKey, ChatUser chatUser);
}
