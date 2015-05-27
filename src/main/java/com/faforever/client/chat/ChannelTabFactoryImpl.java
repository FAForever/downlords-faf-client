package com.faforever.client.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class ChannelTabFactoryImpl implements ChannelTabFactory {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Override
  public ChannelTab createChannelTab(String channelName) {
    ChannelTab channelTab = new ChannelTab(channelName, false);
    applicationContext.autowireBean(channelTab);
    applicationContext.initializeBean(channelTab, channelName + "ChatTab");
    return channelTab;
  }
}
