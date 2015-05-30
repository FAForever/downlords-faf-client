package com.faforever.client.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.lang.invoke.MethodHandles;

public class ChatUserControlFactoryImpl implements ChatUserControlFactory {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Override
  public ChatUserControl createChatUserControl(PlayerInfoBean playerInfoBean, OnChatUserControlDoubleClickListener onChatUserControlDoubleClickListener) {
    ChatUserControl chatUserControl = new ChatUserControl(playerInfoBean, onChatUserControlDoubleClickListener);
    applicationContext.autowireBean(chatUserControl);
    applicationContext.initializeBean(chatUserControl, playerInfoBean.getUsername() + "Control");
    return chatUserControl;
  }
}
