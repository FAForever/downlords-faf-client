package com.faforever.client.chat;

import com.faforever.client.legacy.message.PlayerInfoMessage;
import javafx.collections.ObservableMap;

public interface ChannelTabFactory {

  ChannelTab createChannelTab(String channelName, ObservableMap<String, PlayerInfoMessage> playerInfos);

}
