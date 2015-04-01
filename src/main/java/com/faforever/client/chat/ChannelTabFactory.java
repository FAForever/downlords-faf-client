package com.faforever.client.chat;

import com.faforever.client.legacy.message.PlayerInfo;
import javafx.collections.ObservableMap;

public interface ChannelTabFactory {

  ChannelTab createChannelTab(String channelName, ObservableMap<String, PlayerInfo> playerInfos);

}
