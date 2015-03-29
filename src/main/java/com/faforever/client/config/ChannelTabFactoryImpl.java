package com.faforever.client.config;

import com.faforever.client.chat.ChannelTab;
import com.faforever.client.chat.ChannelTabFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class ChannelTabFactoryImpl implements ChannelTabFactory {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Override
  public ChannelTab createChannelTab(String channelName) {
    ChannelTab channelTab = new ChannelTab(channelName);
    applicationContext.autowireBean(channelTab);
    applicationContext.initializeBean(channelTab, channelName + "ChatTab");
    return channelTab;
  }
}
