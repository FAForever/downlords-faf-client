package com.faforever.client.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class ChatUserControlFactoryImpl implements ChatUserControlFactory {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Override
  public ChatUserControl newUserEntry(ChatUser chatUser) {
    ChatUserControl chatUserControl = new ChatUserControl(chatUser);
    applicationContext.autowireBean(chatUserControl);
    applicationContext.initializeBean(chatUserControl, chatUser.getNick() + "Control");
    return chatUserControl;
  }
}
