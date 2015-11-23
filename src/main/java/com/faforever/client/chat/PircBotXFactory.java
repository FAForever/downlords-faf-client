package com.faforever.client.chat;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;

public interface PircBotXFactory {

  ShutdownablePircBotX createPircBotX(Configuration<PircBotX> configuration);
}
