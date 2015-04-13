package com.faforever.client.chat;

import javafx.collections.ObservableMap;

public interface ChannelTabFactory {

  ChannelTab createChannelTab(String channelName, ObservableMap<String, PlayerInfoBean> playerInfoMap);

}
