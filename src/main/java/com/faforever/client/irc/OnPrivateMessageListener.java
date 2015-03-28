package com.faforever.client.irc;

import java.time.Instant;

public interface OnPrivateMessageListener {

  void onPrivateMessage(String from, Instant instant, String message);
}
