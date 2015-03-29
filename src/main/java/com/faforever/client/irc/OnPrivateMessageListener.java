package com.faforever.client.irc;

public interface OnPrivateMessageListener {

  void onPrivateMessage(String sender, IrcMessage ircMessage);
}
