package com.faforever.client.chat;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;

public class PircBotXFactoryImpl implements PircBotXFactory {

  @Override
  public ShutdownablePircBotX createPircBotX(Configuration<PircBotX> configuration) {
    return new ShutdownablePircBotX(configuration);
  }
}
