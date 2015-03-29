package com.faforever.client.irc;

public interface OnMessageListener {

  void onMessage(String channelName, IrcMessage ircMessage);
}
