package com.faforever.client.chat;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;

public class ShutdownablePircBotX extends PircBotX {

  /**
   * Constructs a PircBotX with the provided configuration.
   */
  public ShutdownablePircBotX(Configuration<? extends PircBotX> configuration) {
    super(configuration);
  }

  @Override
  public void shutdown() {
    super.shutdown();
  }
}
