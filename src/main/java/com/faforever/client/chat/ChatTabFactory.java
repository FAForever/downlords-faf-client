package com.faforever.client.chat;

public interface ChatTabFactory {

  AbstractChatTab createChannelTab(String channelName);

  AbstractChatTab createPrivateMessageTab(String username);
}
