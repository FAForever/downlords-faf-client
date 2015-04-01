package com.faforever.client.chat;

import com.faforever.client.legacy.message.PlayerInfo;
import javafx.collections.ObservableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class ChannelTabFactoryImpl implements ChannelTabFactory {

  @Autowired
  AutowireCapableBeanFactory applicationContext;

  @Override
  public ChannelTab createChannelTab(String channelName, ObservableMap<String, PlayerInfo> playerInfoMap) {
    ChannelTab channelTab = new ChannelTab(channelName, playerInfoMap);
    applicationContext.autowireBean(channelTab);
    applicationContext.initializeBean(channelTab, channelName + "ChatTab");
    return channelTab;
  }
}
