package com.faforever.client.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class ChatUserControlFactoryImpl implements ChatUserControlFactory {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Override
  public ChatUserControl newChatUserControl(PlayerInfoBean playerInfoBean) {
    ChatUserControl chatUserControl = new ChatUserControl(playerInfoBean);
    applicationContext.autowireBean(chatUserControl);
    applicationContext.initializeBean(chatUserControl, playerInfoBean.getUsername() + "Control");
    return chatUserControl;
  }
}
