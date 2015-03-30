package com.faforever.client.chat;

public interface OnPrivateMessageListener {

  void onPrivateMessage(String sender, ChatMessage chatMessage);
}
