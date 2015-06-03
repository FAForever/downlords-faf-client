package com.faforever.client.chat;

public interface OnChatMessageListener {

  void onMessage(String channelName, ChatMessage chatMessage);
}
