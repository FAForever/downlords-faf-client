package com.faforever.client.chat;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class PircBotXFactoryImpl implements PircBotXFactory {

  @Override
  public PircBotX createPircBotX(Configuration configuration) {
    return new PircBotX(configuration);
  }
}
