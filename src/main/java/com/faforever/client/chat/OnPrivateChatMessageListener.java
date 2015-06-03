package com.faforever.client.chat;

public interface OnPrivateChatMessageListener {

  void onPrivateMessage(String sender, ChatMessage chatMessage);
}
