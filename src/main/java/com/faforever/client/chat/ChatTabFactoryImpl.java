package com.faforever.client.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class ChatTabFactoryImpl implements ChatTabFactory {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Override
  public AbstractChatTab createChannelTab(String channelName) {
    AbstractChatTab chatTab = new ChannelTab(channelName);
    applicationContext.autowireBean(chatTab);
    applicationContext.initializeBean(chatTab, channelName + "ChatTab");
    return chatTab;
  }

  @Override
  public AbstractChatTab createPrivateMessageTab(String username) {
    AbstractChatTab chatTab = new PrivateChatTab(username);
    applicationContext.autowireBean(chatTab);
    applicationContext.initializeBean(chatTab, username + "ChatTab");
    return chatTab;
  }
}
