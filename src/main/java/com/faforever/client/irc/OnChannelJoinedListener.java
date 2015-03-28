package com.faforever.client.irc;

import java.util.List;

public interface OnChannelJoinedListener {

  void onChannelJoined(String channelKey, List<IrcUser> users);
}
