package com.faforever.client.chat;

public interface OnMessageListener {

  void onMessage(String channelName, ChatMessage chatMessage);
}
