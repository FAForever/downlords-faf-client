package com.faforever.client.irc;

import java.time.Instant;

public interface OnMessageListener {

  void onMessage(String channelName, Instant instant, String sender, String message);
}
